package com.together.profiler.services;

import com.together.profiler.dto.ProfileDto;
import com.together.profiler.entity.Interest;
import com.together.profiler.entity.UserInterest;
import com.together.profiler.entity.UserProfile;
import com.together.profiler.exceptions.NotFoundException;
import com.together.profiler.repository.UserProfileRepository;
import com.together.profiler.storage.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserProfileRepository profileRepository;
    private final S3StorageService         storageService;

    // ── GET /profile/me ─────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProfileDto.Response getMyProfile(Long userId, String phone) {
        UserProfile profile = findOrCreate(userId);
        return toResponse(profile, phone);
    }

    /** Публичный профиль по userId — для просмотра профиля партнёра. Телефон не возвращается. */
    @Transactional(readOnly = true)
    public ProfileDto.Response getProfileByUserId(Long userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        return toResponse(profile, null); // phone = null — не показываем чужой телефон
    }

    // ── PUT /profile/me ─────────────────────────────────────────────────────
    @Transactional
    public ProfileDto.Response updateProfile(Long userId, String phone, ProfileDto.UpdateRequest req) {
        UserProfile profile = findOrCreate(userId);

        if (req.getName()      != null) profile.setName(req.getName());
        if (req.getAge()       != null) profile.setAge(req.getAge());
        if (req.getCity()      != null) profile.setCity(req.getCity());
        if (req.getInterests() != null) replaceInterests(profile, req.getInterests());

        return toResponse(profileRepository.save(profile), phone);
    }

    // ── POST /profile/me/avatar ──────────────────────────────────────────────
    @Transactional
    public ProfileDto.Response uploadAvatar(Long userId, String phone, MultipartFile file) throws IOException {
        UserProfile profile = findOrCreate(userId);

        // Удаляем старый аватар если был
        if (profile.getAvatarS3Key() != null) {
            storageService.delete(profile.getAvatarS3Key());
        }

        String key = storageService.upload(file, "avatars/" + userId);
        profile.setAvatarS3Key(key);

        return toResponse(profileRepository.save(profile), phone);
    }

    // ── POST /profile/me/onboarding ──────────────────────────────────────────
    // Вызывается на каждом шаге онбординга — сохраняет прогресс
    @Transactional
    public ProfileDto.Response saveOnboardingStep(Long userId, String phone, ProfileDto.OnboardingStepRequest req) {
        UserProfile profile = findOrCreate(userId);

        // Применяем только заполненные поля текущего шага
        if (req.getName()      != null) profile.setName(req.getName());
        if (req.getAge()       != null) profile.setAge(req.getAge());
        if (req.getCity()      != null) profile.setCity(req.getCity());
        if (req.getInterests() != null) replaceInterests(profile, req.getInterests());

        // Обновляем прогресс
        profile.setOnboardingStep(req.getStep());

        // Шаг 4 — финальный экран, онбординг завершён
        if (req.getStep() >= 4) {
            profile.setOnboardingCompleted(true);
            log.info("Onboarding completed for userId={}", userId);
        }

        return toResponse(profileRepository.save(profile), phone);
    }

    // ── Хелперы ─────────────────────────────────────────────────────────────

    // Создаём профиль при первом обращении (lazy init)
    private UserProfile findOrCreate(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new profile for userId={}", userId);
                    return profileRepository.save(
                            UserProfile.builder().userId(userId).build()
                    );
                });
    }

    private void replaceInterests(UserProfile profile, Set<Interest> interests) {
        profile.getInterests().clear();
        interests.forEach(interest ->
                profile.getInterests().add(
                        UserInterest.builder()
                                .userProfile(profile)
                                .interest(interest)
                                .build()
                )
        );
    }

    private ProfileDto.Response toResponse(UserProfile p, String phone) {
        String avatarUrl = p.getAvatarS3Key() != null
                ? storageService.getPublicUrl(p.getAvatarS3Key())
                : null;

        List<Interest> interests = p.getInterests().stream()
                .map(UserInterest::getInterest)
                .collect(Collectors.toList());

        return ProfileDto.Response.builder()
                .userId(p.getUserId())
                .phone(phone)
                .name(p.getName())
                .age(p.getAge())
                .city(p.getCity())
                .avatarUrl(avatarUrl)
                .interests(interests)
                .onboardingCompleted(p.getOnboardingCompleted())
                .onboardingStep(p.getOnboardingStep())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}