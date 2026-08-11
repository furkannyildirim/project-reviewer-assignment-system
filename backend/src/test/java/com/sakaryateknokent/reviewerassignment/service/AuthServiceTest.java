package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CurrentUserResponse;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import com.sakaryateknokent.reviewerassignment.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthService service = new AuthService(
            mock(AuthenticationManager.class),
            mock(JwtTokenService.class),
            userRepository
    );

    @Test
    void returnsCurrentUserByAuthenticatedEmail() {
        User user = activeUser();
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        CurrentUserResponse response = service.getCurrentUser(
                "user@example.com"
        );

        assertEquals(1L, response.id());
        assertEquals("Ada", response.firstName());
        assertEquals("Lovelace", response.lastName());
        assertEquals("user@example.com", response.email());
        assertEquals("555 11", response.phone());
        assertEquals(Role.USER, response.role());
        assertEquals(true, response.active());
    }

    @Test
    void rejectsWhenCurrentUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.getCurrentUser("missing@example.com")
        );
    }

    @Test
    void rejectsWhenCurrentUserIsInactive() {
        User user = activeUser();
        user.setActive(false);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                DisabledException.class,
                () -> service.getCurrentUser("user@example.com")
        );
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setEmail("user@example.com");
        user.setPhone("555 11");
        user.setRole(Role.USER);
        user.setActive(true);
        return user;
    }
}
