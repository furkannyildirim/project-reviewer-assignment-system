package com.sakaryateknokent.reviewerassignment.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "project.upload.cleanup")
public record ProjectUploadCleanupProperties(
        @NotNull Duration interval,
        @Min(1) int batchSize
) {
}
