package com.sakaryateknokent.reviewerassignment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReviewerAssignmentRequest(
        @NotNull(message = "Akademisyen zorunludur")
        @Positive(message = "Akademisyen kimligi pozitif olmalidir")
        Long academicianId
) {
}
