package com.sakaryateknokent.reviewerassignment.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank(message = "JWT anahtarı boş olamaz")
        String secret,

        @NotBlank(message = "JWT issuer boş olamaz")
        String issuer,

        @NotNull(message = "JWT access token süresi belirtilmelidir")
        Duration accessTokenExpiration

) {
}