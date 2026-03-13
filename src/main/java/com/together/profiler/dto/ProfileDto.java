package com.together.profiler.dto;

import com.together.profiler.entity.Interest;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ProfileDto {

    // ── Ответ: профиль пользователя ───────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long userId;
        private String phone;          // из X-User-Phone заголовка (auth-service)
        private String name;
        private Integer age;
        private String city;
        private String avatarUrl;          // публичный URL, строится из avatarS3Key
        private List<Interest> interests;
        private Boolean onboardingCompleted;
        private Integer onboardingStep;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // ── Запрос: обновить профиль ──────────────────────────────────────────
    @Data
    public static class UpdateRequest {
        @Size(min = 1, max = 100, message = "Name must be 1–100 characters")
        private String name;

        @Min(value = 1, message = "Age must be positive")
        @Max(value = 120, message = "Age seems incorrect")
        private Integer age;

        @Size(max = 100, message = "City name too long")
        private String city;

        @Size(max = 9, message = "Max 9 interests")
        private Set<Interest> interests;
    }

    // ── Запрос: онбординг (все шаги в одном запросе или поэтапно) ─────────
    @Data
    public static class OnboardingStepRequest {
        // Шаг 0 — имя
        @Size(min = 1, max = 100)
        private String name;

        // Шаг 1 — возраст
        @Min(1) @Max(120)
        private Integer age;

        // Шаг 2 — интересы
        @Size(max = 9)
        private Set<Interest> interests;

        // Шаг 3 — город
        @Size(max = 100)
        private String city;

        // Текущий шаг (0–4), сохраняется для возобновления
        @NotNull
        @Min(0) @Max(4)
        private Integer step;
    }
}