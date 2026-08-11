package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.UpdateUserRequest;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService service = new UserService(
            userRepository,
            mock(PasswordEncoder.class)
    );

    @Test
    void incrementsTokenVersionWhenEmailChanges() {
        User user = user();
        when(userRepository.findByIdAndRole(1L, Role.USER))
                .thenReturn(Optional.of(user));

        service.updateUser(1L, request("new@example.com", true));

        assertEquals(5L, user.getTokenVersion());
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void incrementsTokenVersionWhenActiveChanges() {
        User user = user();
        when(userRepository.findByIdAndRole(1L, Role.USER))
                .thenReturn(Optional.of(user));

        service.updateUser(1L, request("user@example.com", false));

        assertEquals(5L, user.getTokenVersion());
    }

    @Test
    void doesNotIncrementTokenVersionWhenOnlyProfileFieldsChange() {
        User user = user();
        when(userRepository.findByIdAndRole(1L, Role.USER))
                .thenReturn(Optional.of(user));

        service.updateUser(1L, request(" USER@EXAMPLE.COM ", true));

        assertEquals(4L, user.getTokenVersion());
        assertEquals("Yeni", user.getFirstName());
        assertEquals("Soyad", user.getLastName());
        assertEquals("555 22", user.getPhone());
        assertEquals("user@example.com", user.getEmail());
    }

    private UpdateUserRequest request(String email, boolean active) {
        return new UpdateUserRequest(
                " Yeni ",
                " Soyad ",
                email,
                " 555 22 ",
                active
        );
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("user@example.com");
        user.setPhone("555 11");
        user.setPasswordHash("old-hash");
        user.setRole(Role.USER);
        user.setActive(true);
        user.setTokenVersion(4L);
        return user;
    }
}
