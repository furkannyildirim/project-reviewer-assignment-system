package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ProjectCompletionService {

    private final ProjectRepository projectRepository;
    private final ProjectReviewerAssignmentRepository assignmentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean completeIfDue(
            Long projectId,
            LocalDate businessDate,
            ZoneId businessZone
    ) {
        Project project = projectRepository.findByIdForReviewerAssignment(projectId)
                .orElse(null);
        if (project == null || !isEligibleStatus(project.getStatus())) {
            return false;
        }

        LocalDate endDate = project.getEndDate();
        if (endDate == null || endDate.isAfter(businessDate)) {
            return false;
        }

        Instant effectiveCompletionTime = endDate.atStartOfDay(businessZone).toInstant();
        assignmentRepository.completeActiveAssignments(
                projectId,
                AssignmentStatus.ACTIVE,
                AssignmentStatus.COMPLETED,
                effectiveCompletionTime
        );
        project.setStatus(ProjectStatus.COMPLETED);
        projectRepository.saveAndFlush(project);
        return true;
    }

    private boolean isEligibleStatus(ProjectStatus status) {
        return status == ProjectStatus.AWAITING_REVIEWER
                || status == ProjectStatus.ACTIVE;
    }
}
