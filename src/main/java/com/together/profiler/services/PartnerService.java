package com.together.profiler.services;

import com.together.profiler.dto.PartnerDto;
import com.together.profiler.entity.InviteCode;
import com.together.profiler.entity.InviteCode.InviteStatus;
import com.together.profiler.entity.Partnership;
import com.together.profiler.entity.UserProfile;
import com.together.profiler.exceptions.BadRequestException;
import com.together.profiler.exceptions.NotFoundException;
import com.together.profiler.repository.InviteCodeRepository;
import com.together.profiler.repository.PartnershipRepository;
import com.together.profiler.repository.UserProfileRepository;
import com.together.profiler.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnershipRepository partnershipRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final UserProfileRepository profileRepository;
    private final S3StorageService storageService;

    @Value("${invite.base-url}")
    private String inviteBaseUrl;

    @Value("${invite.code-length:8}")
    private int codeLength;

    @Value("${invite.ttl-days:7}")
    private int ttlDays;

    // ── GET /partner ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Optional<PartnerDto.PartnerResponse> getPartner(Long userId) {
        return partnershipRepository.findActiveByUserId(userId)
                .map(p -> buildPartnerResponse(p.getPartnerIdFor(userId), p.getCreatedAt()));
    }

    // ── POST /partner/invite ──────────────────────────────────────────────────
    // Создаёт новый инвайт-код (или возвращает существующий PENDING)
    @Transactional
    public PartnerDto.InviteResponse createInvite(Long userId) {
        // Нельзя приглашать если уже есть партнёр
        if (partnershipRepository.findActiveByUserId(userId).isPresent()) {
            throw new BadRequestException("You already have a partner");
        }

        // Проверяем есть ли уже активный инвайт
        Optional<InviteCode> existing = inviteCodeRepository
                .findByInviterIdAndStatus(userId, InviteStatus.PENDING);

        if (existing.isPresent() && !existing.get().isExpired()) {
            return toInviteResponse(existing.get());
        }

        // Создаём новый
        InviteCode invite = InviteCode.builder()
                .inviterId(userId)
                .code(generateCode())
                .status(InviteStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(ttlDays))
                .build();

        inviteCodeRepository.save(invite);
        log.info("Invite created: code={}, inviter={}", invite.getCode(), userId);
        return toInviteResponse(invite);
    }

    // ── POST /partner/invite/{code}/accept ────────────────────────────────────
    @Transactional
    public PartnerDto.AcceptInviteResponse acceptInvite(String code, Long acceptorId) {
        InviteCode invite = inviteCodeRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Invite not found"));

        // Валидация
        if (!invite.isUsable()) {
            throw new BadRequestException(
                    invite.isExpired() ? "Invite has expired" : "Invite is no longer valid");
        }
        if (invite.getInviterId().equals(acceptorId)) {
            throw new BadRequestException("You cannot accept your own invite");
        }
        if (partnershipRepository.findActiveByUserId(acceptorId).isPresent()) {
            throw new BadRequestException("You already have a partner");
        }
        if (partnershipRepository.existsActiveBetween(invite.getInviterId(), acceptorId)) {
            throw new BadRequestException("Partnership already exists");
        }

        // Помечаем инвайт принятым
        invite.setStatus(InviteStatus.ACCEPTED);
        invite.setAcceptedById(acceptorId);
        invite.setAcceptedAt(LocalDateTime.now());
        inviteCodeRepository.save(invite);

        // Создаём партнёрскую связь (user1Id < user2Id — исключаем дубли)
        Long u1 = Math.min(invite.getInviterId(), acceptorId);
        Long u2 = Math.max(invite.getInviterId(), acceptorId);
        Partnership partnership = Partnership.builder()
                .user1Id(u1)
                .user2Id(u2)
                .build();
        partnershipRepository.save(partnership);

        log.info("Partnership created: user1={}, user2={}", u1, u2);

        // Возвращаем данные о новом партнёре
        UserProfile inviterProfile = profileRepository
                .findByUserId(invite.getInviterId()).orElse(null);

        return PartnerDto.AcceptInviteResponse.builder()
                .partnerUserId(invite.getInviterId())
                .partnerName(inviterProfile != null ? inviterProfile.getName() : null)
                .partnerAvatarUrl(buildAvatarUrl(inviterProfile))
                .connectedAt(partnership.getCreatedAt())
                .build();
    }

    // ── DELETE /partner ───────────────────────────────────────────────────────
    @Transactional
    public void removePartner(Long userId) {
        Partnership partnership = partnershipRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No active partner"));

        partnership.setDeletedAt(LocalDateTime.now());
        partnershipRepository.save(partnership);
        log.info("Partnership dissolved by userId={}", userId);
    }

    // ── Хелперы ──────────────────────────────────────────────────────────────

    private PartnerDto.PartnerResponse buildPartnerResponse(Long partnerUserId,
                                                            LocalDateTime connectedAt) {
        UserProfile profile = profileRepository.findByUserId(partnerUserId).orElse(null);
        return PartnerDto.PartnerResponse.builder()
                .partnerUserId(partnerUserId)
                .partnerName(profile != null ? profile.getName() : null)
                .partnerAvatarUrl(buildAvatarUrl(profile))
                .connectedAt(connectedAt)
                .build();
    }

    private String buildAvatarUrl(UserProfile profile) {
        if (profile == null || profile.getAvatarS3Key() == null) return null;
        return storageService.getPublicUrl(profile.getAvatarS3Key());
    }

    private PartnerDto.InviteResponse toInviteResponse(InviteCode invite) {
        return PartnerDto.InviteResponse.builder()
                .code(invite.getCode())
                .inviteUrl(inviteBaseUrl + "/join/" + invite.getCode())
                .status(invite.getStatus())
                .expiresAt(invite.getExpiresAt())
                .build();
    }

    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    private String generateCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}