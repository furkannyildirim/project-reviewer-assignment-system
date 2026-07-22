package com.sakaryateknokent.reviewerassignment.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.core.AuthenticationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            String message = fieldError.getDefaultMessage() != null
                    ? fieldError.getDefaultMessage()
                    : "Geçersiz değer";

            fieldErrors.putIfAbsent(fieldError.getField(), message);
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "İstek alanlarında doğrulama hataları var",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "İstek gövdesi application/json formatında gönderilmelidir",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Veritabanı bütünlük kısıtı ihlal edildi: {}",
                exception.getMostSpecificCause().getMessage()
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "İşlem veritabanı kısıtları nedeniyle tamamlanamadı",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "E-posta veya parola hatalı",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Beklenmeyen bir hata oluştu", exception);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Beklenmeyen bir hata oluştu",
                request.getRequestURI(),
                Map.of()
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                fieldErrors
        );

        return ResponseEntity.status(status).body(response);
    }
}