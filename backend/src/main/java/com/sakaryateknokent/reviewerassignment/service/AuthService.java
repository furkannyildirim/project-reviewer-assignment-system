package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.LoginRequest;
import com.sakaryateknokent.reviewerassignment.dto.LoginResponse;
import com.sakaryateknokent.reviewerassignment.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        String normalizedEmail = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                normalizedEmail,
                                request.password()
                        )
                );

        String accessToken = jwtTokenService.generateToken(authentication);

        return new LoginResponse(
                accessToken,
                TOKEN_TYPE,
                jwtTokenService.getAccessTokenExpirationSeconds()
        );
    }
}