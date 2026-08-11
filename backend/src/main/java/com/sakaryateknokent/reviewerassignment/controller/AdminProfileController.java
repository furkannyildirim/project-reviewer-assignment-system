package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.AdminProfileResponse;
import com.sakaryateknokent.reviewerassignment.dto.ChangeAdminPasswordRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAdminProfileRequest;
import com.sakaryateknokent.reviewerassignment.service.AdminProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
public class AdminProfileController {

    private final AdminProfileService adminProfileService;

    @GetMapping
    public ResponseEntity<AdminProfileResponse> getProfile(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                adminProfileService.getProfile(authentication.getName())
        );
    }

    @PutMapping
    public ResponseEntity<AdminProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateAdminProfileRequest request
    ) {
        return ResponseEntity.ok(adminProfileService.updateProfile(
                authentication.getName(),
                request
        ));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangeAdminPasswordRequest request
    ) {
        adminProfileService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
