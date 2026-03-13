package com.together.profiler.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Инвайт-код для добавления партнёра.
 *
 * Флоу:
 * 1. Пользователь A создаёт инвайт → получает ссылку vmeste.app/join/{code}
 * 2. Пользователь B переходит по ссылке → принимает инвайт
 * 3. Создаётся Partnership, инвайт помечается как использованный
 */
@Entity
@Table(name = "invite_codes", schema = "profiler")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Кто создал инвайт
    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    // Короткий уникальный код: a8k2x9mf
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InviteStatus status = InviteStatus.PENDING;

    // Кто принял инвайт (заполняется при принятии)
    @Column(name = "accepted_by_id")
    private Long acceptedById;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Срок действия — по умолчанию 7 дней
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    // ── Хелперы ────────────────────────────────────────────────────────────
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return status == InviteStatus.PENDING && !isExpired();
    }

    public enum InviteStatus {
        PENDING,
        ACCEPTED,
        EXPIRED,
        CANCELLED
    }
}