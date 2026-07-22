package com.sakaryateknokent.reviewerassignment.security;

import com.sakaryateknokent.reviewerassignment.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RestSecurityErrorHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");

        writeResponse(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Kimlik doğrulama gereklidir"
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        writeResponse(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Bu işlem için yetkiniz bulunmuyor"
        );
    }

    private void writeResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        );

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                errorResponse
        );
    }
}