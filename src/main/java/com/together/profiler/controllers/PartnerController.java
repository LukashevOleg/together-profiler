package com.together.profiler.controllers;

import com.together.profiler.dto.PartnerDto;
import com.together.profiler.services.PartnerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/partner")
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    // GET /api/partner — получить текущего партнёра
    @GetMapping
    public ResponseEntity<PartnerDto.PartnerResponse> getPartner(
            @RequestHeader("X-User-Id") Long userId) {
        return partnerService.getPartner(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build()); // 204 — партнёра нет
    }

    // POST /api/partner/invite — создать инвайт-ссылку
    @PostMapping("/invite")
    public ResponseEntity<PartnerDto.InviteResponse> createInvite(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(partnerService.createInvite(userId));
    }

    // POST /api/partner/invite/{code}/accept — принять инвайт
    @PostMapping("/invite/{code}/accept")
    public ResponseEntity<PartnerDto.AcceptInviteResponse> acceptInvite(
            @PathVariable String code,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(partnerService.acceptInvite(code, userId));
    }

    // DELETE /api/partner — разорвать связь
    @DeleteMapping
    public ResponseEntity<Void> removePartner(
            @RequestHeader("X-User-Id") Long userId) {
        partnerService.removePartner(userId);
        return ResponseEntity.noContent().build();
    }
}