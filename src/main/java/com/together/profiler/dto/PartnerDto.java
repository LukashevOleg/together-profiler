package com.together.profiler.dto;

import com.together.profiler.entity.InviteCode.InviteStatus;
import lombok.*;

import java.time.LocalDateTime;

public class PartnerDto {

    // ── Ответ: текущий партнёр ────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PartnerResponse {
        private Long partnerUserId;
        private String partnerName;
        private String partnerAvatarUrl;
        private LocalDateTime connectedAt;
    }

    // ── Ответ: созданный инвайт ───────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class InviteResponse {
        private String code;
        private String inviteUrl;
        private InviteStatus status;
        private LocalDateTime expiresAt;
    }

    // ── Ответ: принятие инвайта ───────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AcceptInviteResponse {
        private Long partnerUserId;
        private String partnerName;
        private String partnerAvatarUrl;
        private LocalDateTime connectedAt;
    }
}