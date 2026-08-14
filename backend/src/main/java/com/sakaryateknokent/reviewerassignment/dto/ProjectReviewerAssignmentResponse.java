package com.sakaryateknokent.reviewerassignment.dto;

import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;

import java.time.Instant;

public record ProjectReviewerAssignmentResponse(
        Long assignmentId,
        Long projectId,
        Long academicianId,
        String academicianName,
        String academicianTitle,
        AssignmentStatus status,
        Instant assignedAt,
        String assignedByName
) {
}
