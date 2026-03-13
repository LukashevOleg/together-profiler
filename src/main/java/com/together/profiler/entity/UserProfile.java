package com.together.profiler.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Профиль пользователя.
 * userId — это ID из auth-service, прокидывается Gateway через X-User-Id.
 */
@Entity
@Table(name = "user_profiles", schema = "profiler")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Внешний ключ на auth-service.users — только ID, без JOIN
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "age")
    private Integer age;

    @Column(name = "city", length = 100)
    private String city;

    // Ключ в S3/MinIO, публичный URL строится через StorageService
    @Column(name = "avatar_s3_key")
    private String avatarS3Key;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private Boolean onboardingCompleted = false;

    // Шаг, на котором пользователь остановился (0-4)
    // Нужен для возобновления онбординга после перезапуска приложения
    @Column(name = "onboarding_step", nullable = false)
    @Builder.Default
    private Integer onboardingStep = 0;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<UserInterest> interests = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}