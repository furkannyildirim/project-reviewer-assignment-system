package com.sakaryateknokent.reviewerassignment.config;

import com.sakaryateknokent.reviewerassignment.security.JwtProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtConfig {

    private static final int MINIMUM_KEY_LENGTH_BYTES = 32;

    @Bean
    public SecretKey jwtSecretKey(JwtProperties jwtProperties) {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(jwtProperties.secret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT_SECRET geçerli bir Base64 değeri olmalıdır",
                    exception
            );
        }

        if (keyBytes.length < MINIMUM_KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET en az 32 byte uzunluğunda olmalıdır"
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            JwtProperties jwtProperties
    ) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        jwtDecoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(jwtProperties.issuer())
        );

        return jwtDecoder;
    }
}