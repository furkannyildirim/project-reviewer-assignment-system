package com.sakaryateknokent.reviewerassignment.security;

import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.Role;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DbAwareJwtAuthenticationConverterTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final DbAwareJwtAuthenticationConverter converter =
            new DbAwareJwtAuthenticationConverter(userRepository);

    @Test
    void acceptsActiveUserWithMatchingVersionAndQueriesByIdOnce() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        JwtAuthenticationToken authentication = convert(jwt("1", 4L));

        assertTrue(authentication.isAuthenticated());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void usesCurrentDatabaseEmailAsAuthenticationName() {
        User user = user();
        user.setEmail("current@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JwtAuthenticationToken authentication = convert(jwt("1", 4L));

        assertEquals("current@example.com", authentication.getName());
    }

    @Test
    void usesCurrentDatabaseRoleAsAuthority() {
        User user = user();
        user.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        JwtAuthenticationToken authentication = convert(jwt("1", 4L));

        assertEquals(
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                List.copyOf(authentication.getAuthorities())
        );
    }

    @Test
    void rejectsLegacyEmailSubject() {
        assertRejected(jwt("user@example.com", 4L));
    }

    @Test
    void rejectsMissingSubject() {
        assertRejected(jwt(null, 4L));
    }

    @Test
    void rejectsBlankSubject() {
        assertRejected(jwt(" ", 4L));
    }

    @Test
    void rejectsNonNumericSubject() {
        assertRejected(jwt("not-an-id", 4L));
    }

    @Test
    void rejectsInvalidUserIdSubject() {
        assertRejected(jwt("0", 4L));
    }

    @Test
    void rejectsWhenUserDoesNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertRejected(jwt("1", 4L));
    }

    @Test
    void rejectsInactiveUser() {
        User user = user();
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertRejected(jwt("1", 4L));
    }

    @Test
    void rejectsMissingTokenVersionClaim() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        assertRejected(jwt("1", null));
    }

    @Test
    void rejectsNonNumericTokenVersionClaim() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        assertRejected(jwt("1", "4"));
    }

    @Test
    void rejectsMismatchedTokenVersion() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        assertRejected(jwt("1", 3L));
    }

    private void assertRejected(Jwt jwt) {
        assertThrows(
                OAuth2AuthenticationException.class,
                () -> converter.convert(jwt)
        );
    }

    private JwtAuthenticationToken convert(Jwt jwt) {
        return (JwtAuthenticationToken) converter.convert(jwt);
    }

    private Jwt jwt(String subject, Object tokenVersion) {
        Instant issuedAt = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(900));

        if (subject != null) {
            builder.subject(subject);
        }

        if (tokenVersion != null) {
            builder.claim("tokenVersion", tokenVersion);
        }

        return builder.build();
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setRole(Role.USER);
        user.setActive(true);
        user.setTokenVersion(4L);
        return user;
    }
}
