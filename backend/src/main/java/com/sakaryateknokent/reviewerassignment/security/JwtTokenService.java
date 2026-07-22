package com.sakaryateknokent.reviewerassignment.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public String generateToken(Authentication authentication) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(
                jwtProperties.accessTokenExpiration()
        );

        String role = extractRole(authentication);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(authentication.getName())
                .claim("roles", List.of(role))
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpiration().toSeconds();
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Kullanıcının rol bilgisi bulunamadı"
                ));
    }
}