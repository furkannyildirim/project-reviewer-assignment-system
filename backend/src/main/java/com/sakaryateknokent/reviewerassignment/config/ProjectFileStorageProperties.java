package com.sakaryateknokent.reviewerassignment.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "project.file.storage")
public record ProjectFileStorageProperties(
        @NotNull Path path,
        @NotNull DataSize maxSize,
        @NotNull Duration uploadTtl
) {
}
