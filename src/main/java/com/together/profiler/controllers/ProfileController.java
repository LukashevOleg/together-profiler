package com.together.profiler.controllers;

import com.together.profiler.dto.ProfileDto;
import com.together.profiler.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // GET /api/profile/me
    @GetMapping("/me")
    public ResponseEntity<ProfileDto.Response> getMyProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Phone", required = false) String phone) {
        return ResponseEntity.ok(profileService.getMyProfile(userId, phone));
    }

    // PUT /api/profile/me
    @PutMapping("/me")
    public ResponseEntity<ProfileDto.Response> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Phone", required = false) String phone,
            @Valid @RequestBody ProfileDto.UpdateRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(userId, phone, request));
    }

    // POST /api/profile/me/avatar
    @PostMapping("/me/avatar")
    public ResponseEntity<ProfileDto.Response> uploadAvatar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Phone", required = false) String phone,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(profileService.uploadAvatar(userId, phone, file));
    }

    // POST /api/profile/me/onboarding
    @PostMapping("/me/onboarding")
    public ResponseEntity<ProfileDto.Response> onboardingStep(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Phone", required = false) String phone,
            @Valid @RequestBody ProfileDto.OnboardingStepRequest request) {
        return ResponseEntity.ok(profileService.saveOnboardingStep(userId, phone, request));
    }
}
