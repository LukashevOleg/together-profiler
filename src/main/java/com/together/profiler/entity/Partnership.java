package com.together.profiler.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * пара двух пользователей.
 *
 * Правила:
 * - у каждого пользователя не более одного активного партнёра
 * - удаление — мягкое через deletedAt (история сохраняется)
 */
@Entity
@Table(name = "partnerships", schema = "profiler",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Partnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Мягкое удаление — партнёр разорвал связь
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── Хелпер: активна ли связь ──────────────────────────────────────────
    public boolean isActive() {
        return deletedAt == null;
    }

    // ── Хелпер: получить ID партнёра зная свой ID ──────────────────────────
    public Long getPartnerIdFor(Long userId) {
        return user1Id.equals(userId) ? user2Id : user1Id;
    }
}