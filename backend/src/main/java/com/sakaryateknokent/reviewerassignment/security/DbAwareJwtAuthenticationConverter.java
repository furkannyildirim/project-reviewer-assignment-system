package com.sakaryateknokent.reviewerassignment.security;

import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DbAwareJwtAuthenticationConverter implements
        Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";
    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw invalidToken();
        }

        String normalizedEmail = subject.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(this::invalidToken);

        if (!user.isActive()) {
            throw invalidToken();
        }

        Object tokenVersionClaim = jwt.getClaims().get(TOKEN_VERSION_CLAIM);

        if (!(tokenVersionClaim instanceof Number tokenVersion)) {
            throw invalidToken();
        }

        if (tokenVersion.longValue() != user.getTokenVersion()) {
            throw invalidToken();
        }

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                ROLE_PREFIX + user.getRole().name()
        );

        return new JwtAuthenticationToken(
                jwt,
                List.of(authority),
                user.getEmail()
        );
    }

    private OAuth2AuthenticationException invalidToken() {
        return new OAuth2AuthenticationException(
                new OAuth2Error(BearerTokenErrorCodes.INVALID_TOKEN),
                "Bearer token geçersiz"
        );
    }
}
