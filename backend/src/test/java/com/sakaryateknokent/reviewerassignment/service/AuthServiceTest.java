package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CurrentUserResponse;
import com.sakaryateknokent.reviewerassignment.dto.LoginRequest;
import com.sakaryateknokent.reviewerassignment.dto.LoginResponse;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import com.sakaryateknokent.reviewerassignment.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AuthenticationManager authenticationManager =
            mock(AuthenticationManager.class);
    private final JwtTokenService jwtTokenService =
            mock(JwtTokenService.class);
    private final AuthService service = new AuthService(
            authenticationManager,
            jwtTokenService,
            userRepository
    );

    @Test
    void generatesLoginTokenWithCurrentUserTokenVersion() {
        Authentication authentication = mock(Authentication.class);
        User user = activeUser();
        user.setTokenVersion(7L);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user));
        when(jwtTokenService.generateToken(authentication, 1L, 7L))
                .thenReturn("access-token");
        when(jwtTokenService.getAccessTokenExpirationSeconds())
                .thenReturn(900L);

        LoginResponse response = service.login(
                new LoginRequest(" USER@EXAMPLE.COM ", "password")
        );

        assertEquals("access-token", response.accessToken());
        assertEquals(900L, response.expiresIn());
        verify(jwtTokenService).generateToken(authentication, 1L, 7L);
    }

    @Test
    void rejectsLoginWhenAuthenticatedUserNoLongerExists() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> service.login(
                        new LoginRequest("missing@example.com", "password")
                )
        );
        verify(jwtTokenService, never()).generateToken(
                any(Authentication.class),
                anyLong(),
                anyLong()
        );
    }

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
