package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerRecommendationsResponse;
import com.sakaryateknokent.reviewerassignment.service.ProjectReviewerRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectReviewerRecommendationController {

    private final ProjectReviewerRecommendationService recommendationService;

    @GetMapping("/{projectId}/reviewer-recommendations")
    public ResponseEntity<ProjectReviewerRecommendationsResponse> getRecommendations(
            @PathVariable Long projectId
    ) {
        return ResponseEntity.ok(recommendationService.getRecommendations(projectId));
    }
}
