package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerRecommendationsResponse;
import com.sakaryateknokent.reviewerassignment.dto.ReviewerRecommendationResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianProjectWorkload;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectReviewerRecommendationService {

    private final ProjectRepository projectRepository;
    private final AcademicianRepository academicianRepository;
    private final ProjectReviewerAssignmentRepository assignmentRepository;
    private final ReviewerRecommendationMatcher matcher;

    @Transactional(readOnly = true)
    public ProjectReviewerRecommendationsResponse getRecommendations(Long projectId) {
        Project project = projectRepository.findForReviewerRecommendationsById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));
        if (project.getStatus() != ProjectStatus.AWAITING_REVIEWER
                && project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidRequestException(
                    "Yalnizca hakem bekleyen veya aktif projelere hakem onerisi alinabilir"
            );
        }

        List<Candidate> matchedCandidates = academicianRepository.findAllWithKeywords()
                .stream()
                .map(academician -> new Candidate(academician,
                        matcher.match(project, academician)))
                .filter(candidate -> candidate.match().isRecommendation())
                .toList();

        if (matchedCandidates.isEmpty()) {
            return new ProjectReviewerRecommendationsResponse(projectId, false, List.of());
        }

        List<Long> academicianIds = matchedCandidates.stream()
                .map(candidate -> candidate.academician().getId())
                .toList();
        Map<Long, AcademicianProjectWorkload> workloads = assignmentRepository.findWorkloads(
                        academicianIds,
                        projectId,
                        AssignmentStatus.ACTIVE,
                        AssignmentStatus.COMPLETED)
                .stream()
                .collect(Collectors.toMap(AcademicianProjectWorkload::getAcademicianId,
                        Function.identity()));

        List<Candidate> orderedCandidates = matchedCandidates.stream()
                .filter(candidate -> !isCurrentlyAssigned(
                        workloads.get(candidate.academician().getId())))
                .sorted(candidateComparator())
                .toList();

        List<ReviewerRecommendationResponse> recommendations = orderedCandidates.stream()
                .map(candidate -> toResponse(candidate,
                        workloads.get(candidate.academician().getId())))
                .toList();
        boolean strongRecommendationAvailable = recommendations.stream()
                .anyMatch(recommendation ->
                        recommendation.level() == ReviewerRecommendationLevel.HIGH);

        return new ProjectReviewerRecommendationsResponse(projectId,
                strongRecommendationAvailable, recommendations);
    }

    private Comparator<Candidate> candidateComparator() {
        return Comparator
                .comparingInt((Candidate candidate) ->
                        candidate.match().level().orElseThrow().ordinal())
                .thenComparing(Comparator.comparingInt((Candidate candidate) ->
                        candidate.match().totalScore()).reversed())
                .thenComparing(Comparator.comparingInt((Candidate candidate) ->
                        candidate.match().directKeywordMatchCount()).reversed())
                .thenComparing(Comparator.comparingInt((Candidate candidate) ->
                        candidate.match().matchedKeywordCount()).reversed())
                .thenComparing(candidate -> ReviewerRecommendationTextNormalizer
                        .normalizePhrase(candidate.academician().getLastName()))
                .thenComparing(candidate -> ReviewerRecommendationTextNormalizer
                        .normalizePhrase(candidate.academician().getFirstName()))
                .thenComparing(candidate -> candidate.academician().getId());
    }

    private boolean isCurrentlyAssigned(AcademicianProjectWorkload workload) {
        return workload != null && workload.getCurrentProjectActiveAssignmentCount() > 0;
    }

    private ReviewerRecommendationResponse toResponse(
            Candidate candidate,
            AcademicianProjectWorkload workload
    ) {
        Academician academician = candidate.academician();
        long activeProjectCount = workload == null ? 0 : workload.getActiveProjectCount();
        long totalProjectCount = workload == null ? 0 : workload.getTotalProjectCount();
        return new ReviewerRecommendationResponse(
                academician.getId(), academician.getFirstName(), academician.getLastName(),
                academician.getTitle(), candidate.match().level().orElseThrow(),
                candidate.match().matchedKeywords(), activeProjectCount, totalProjectCount);
    }

    private record Candidate(
            Academician academician,
            ReviewerRecommendationMatch match
    ) {
    }
}
