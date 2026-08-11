package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AdminProfileResponse;
import com.sakaryateknokent.reviewerassignment.dto.ChangeAdminPasswordRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAdminProfileRequest;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AdminProfileResponse getProfile(String authenticatedEmail) {
        return toResponse(findAuthenticatedAdmin(authenticatedEmail));
    }

    public AdminProfileResponse updateProfile(
            String authenticatedEmail,
            UpdateAdminProfileRequest request
    ) {
        User admin = findAuthenticatedAdmin(authenticatedEmail);
        admin.setFirstName(request.firstName().trim());
        admin.setLastName(request.lastName().trim());
        admin.setPhone(request.phone().trim());

        return toResponse(admin);
    }

    public void changePassword(
            String authenticatedEmail,
            ChangeAdminPasswordRequest request
    ) {
        User admin = findAuthenticatedAdmin(authenticatedEmail);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                admin.getPasswordHash()
        )) {
            throw new InvalidRequestException("Mevcut parola hatalı");
        }

        if (passwordEncoder.matches(
                request.newPassword(),
                admin.getPasswordHash()
        )) {
            throw new InvalidRequestException(
                    "Yeni parola mevcut paroladan farklı olmalıdır."
            );
        }

        admin.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User findAuthenticatedAdmin(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Yönetici hesabı bulunamadı"
                ));

        if (user.getRole() != Role.ADMIN) {
            throw new ResourceNotFoundException("Yönetici hesabı bulunamadı");
        }

        return user;
    }

    private AdminProfileResponse toResponse(User admin) {
        return new AdminProfileResponse(
                admin.getId(),
                admin.getFirstName(),
                admin.getLastName(),
                admin.getEmail(),
                admin.getPhone(),
                admin.getRole(),
                admin.isActive()
        );
    }
}
