package com.sakaryateknokent.reviewerassignment.security;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtTokenServiceTest {

    @Test
    void generatesTokenWithCurrentClaimsAndNumericTokenVersion() {
        JwtEncoder jwtEncoder = mock(JwtEncoder.class);
        Jwt encodedJwt = mock(Jwt.class);
        JwtProperties properties = new JwtProperties(
                "test-secret",
                "reviewer-assignment-test",
                Duration.ofMinutes(15)
        );
        JwtTokenService service = new JwtTokenService(jwtEncoder, properties);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        "user@example.com",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(encodedJwt);
        when(encodedJwt.getTokenValue()).thenReturn("encoded-token");

        String token = service.generateToken(authentication, 42L, 7L);

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parametersCaptor.capture());
        JwtClaimsSet claims = parametersCaptor.getValue().getClaims();

        assertEquals("encoded-token", token);
        assertEquals("42", claims.getSubject());
        assertEquals(List.of("USER"), claims.getClaim("roles"));
        Number tokenVersion = claims.getClaim("tokenVersion");
        assertEquals(7L, tokenVersion.longValue());
        assertEquals("reviewer-assignment-test", claims.getClaim("iss"));
        assertEquals(
                Duration.ofMinutes(15),
                Duration.between(claims.getIssuedAt(), claims.getExpiresAt())
        );
    }
}
