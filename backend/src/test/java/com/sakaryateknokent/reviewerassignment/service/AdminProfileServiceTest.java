package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AdminProfileResponse;
import com.sakaryateknokent.reviewerassignment.dto.ChangeAdminPasswordRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAdminProfileRequest;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final AdminProfileService service = new AdminProfileService(
            userRepository,
            passwordEncoder
    );

    @Test
    void returnsAuthenticatedAdminProfileByEmail() {
        User admin = adminUser();
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        AdminProfileResponse response = service.getProfile("admin@example.com");

        assertEquals(1L, response.id());
        assertEquals("Ada", response.firstName());
        assertEquals("admin@example.com", response.email());
        assertEquals(Role.ADMIN, response.role());
    }

    @Test
    void rejectsAuthenticatedNonAdminAccount() {
        User user = adminUser();
        user.setRole(Role.USER);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.getProfile("user@example.com")
        );
    }

    @Test
    void updatesOnlyEditableProfileFields() {
        User admin = adminUser();
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));

        AdminProfileResponse response = service.updateProfile(
                "admin@example.com",
                new UpdateAdminProfileRequest(" Yeni ", " Yönetici ", " 555 22 ")
        );

        assertEquals("Yeni", response.firstName());
        assertEquals("Yönetici", response.lastName());
        assertEquals("555 22", response.phone());
        assertEquals("admin@example.com", response.email());
        assertEquals(Role.ADMIN, response.role());
        assertEquals("old-hash", admin.getPasswordHash());
        assertEquals(0L, admin.getTokenVersion());
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutChangingHash() {
        User admin = adminUser();
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.changePassword(
                        "admin@example.com",
                        new ChangeAdminPasswordRequest("wrong", "new-password")
                )
        );

        assertEquals("Mevcut parola hatalı", exception.getMessage());
        assertEquals("old-hash", admin.getPasswordHash());
        assertEquals(0L, admin.getTokenVersion());
        verify(passwordEncoder, never()).encode("new-password");
    }

    @Test
    void encodesAndStoresNewPasswordAfterCurrentPasswordMatches() {
        User admin = adminUser();
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("current-password", "old-hash"))
                .thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changePassword(
                "admin@example.com",
                new ChangeAdminPasswordRequest(
                        "current-password",
                        "new-password"
                )
        );

        assertEquals("new-hash", admin.getPasswordHash());
        assertEquals(1L, admin.getTokenVersion());
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void rejectsNewPasswordWhenItMatchesCurrentPassword() {
        User admin = adminUser();
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("same-password", "old-hash"))
                .thenReturn(true);

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.changePassword(
                        "admin@example.com",
                        new ChangeAdminPasswordRequest(
                                "same-password",
                                "same-password"
                        )
                )
        );

        assertEquals(
                "Yeni parola mevcut paroladan farklı olmalıdır.",
                exception.getMessage()
        );
        assertEquals("old-hash", admin.getPasswordHash());
        assertEquals(0L, admin.getTokenVersion());
        verify(passwordEncoder, never()).encode("same-password");
        verify(userRepository, never()).save(admin);
    }

    private User adminUser() {
        User admin = new User();
        admin.setId(1L);
        admin.setFirstName("Ada");
        admin.setLastName("Yönetici");
        admin.setEmail("admin@example.com");
        admin.setPhone("555 11");
        admin.setPasswordHash("old-hash");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        return admin;
    }
}
