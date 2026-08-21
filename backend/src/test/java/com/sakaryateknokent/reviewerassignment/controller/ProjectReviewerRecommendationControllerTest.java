package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerRecommendationsResponse;
import com.sakaryateknokent.reviewerassignment.dto.ReviewerRecommendationResponse;
import com.sakaryateknokent.reviewerassignment.enums.ReviewerRecommendationLevel;
import com.sakaryateknokent.reviewerassignment.service.ProjectReviewerRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectReviewerRecommendationControllerTest {

    private final ProjectReviewerRecommendationService service =
            mock(ProjectReviewerRecommendationService.class);
    private final ProjectReviewerRecommendationController controller =
            new ProjectReviewerRecommendationController(service);

    @Test
    void returnsReadOnlyRecommendationResponseFromService() {
        ProjectReviewerRecommendationsResponse result =
                new ProjectReviewerRecommendationsResponse(7L, true, List.of(
                        new ReviewerRecommendationResponse(3L, "Ayşe", "Yılmaz",
                                "Prof. Dr.", ReviewerRecommendationLevel.HIGH,
                                List.of("Robotik"), 2, 5)));
        when(service.getRecommendations(7L)).thenReturn(result);

        var response = controller.getRecommendations(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(result, response.getBody());
        verify(service).getRecommendations(7L);
    }

    @Test
    void preservesEmptyRecommendationResponse() {
        when(service.getRecommendations(8L)).thenReturn(
                new ProjectReviewerRecommendationsResponse(8L, false, List.of()));

        var body = controller.getRecommendations(8L).getBody();

        assertFalse(body.strongRecommendationAvailable());
        assertEquals(List.of(), body.recommendations());
    }
}
