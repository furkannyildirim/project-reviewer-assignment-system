package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactMethodRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyContactRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyProjectSummary;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyProjectSummaryServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final CompanyService companyService = new CompanyService(
            companyRepository,
            mock(CompanyContactRepository.class),
            mock(CompanyContactMethodRepository.class),
            projectRepository
    );

    @Test
    void returnsAllStatusCountsFromSingleProjectAggregation() {
        CompanyProjectSummary summary = mock(CompanyProjectSummary.class);
        when(companyRepository.existsById(7L)).thenReturn(true);
        when(summary.getTotalProjectCount()).thenReturn(9L);
        when(summary.getDraftProjectCount()).thenReturn(1L);
        when(summary.getAwaitingReviewerProjectCount()).thenReturn(2L);
        when(summary.getActiveProjectCount()).thenReturn(3L);
        when(summary.getCompletedProjectCount()).thenReturn(2L);
        when(summary.getCancelledProjectCount()).thenReturn(1L);
        when(projectRepository.findCompanyProjectSummary(
                7L,
                ProjectStatus.DRAFT,
                ProjectStatus.AWAITING_REVIEWER,
                ProjectStatus.ACTIVE,
                ProjectStatus.COMPLETED,
                ProjectStatus.CANCELLED
        )).thenReturn(Optional.of(summary));

        var response = companyService.getProjectSummary(7L);

        assertEquals(7L, response.companyId());
        assertEquals(9L, response.totalProjectCount());
        assertEquals(1L, response.draftProjectCount());
        assertEquals(2L, response.awaitingReviewerProjectCount());
        assertEquals(3L, response.activeProjectCount());
        assertEquals(2L, response.completedProjectCount());
        assertEquals(1L, response.cancelledProjectCount());
        verify(projectRepository).findCompanyProjectSummary(
                7L,
                ProjectStatus.DRAFT,
                ProjectStatus.AWAITING_REVIEWER,
                ProjectStatus.ACTIVE,
                ProjectStatus.COMPLETED,
                ProjectStatus.CANCELLED
        );
        verify(projectRepository, never()).save(any());
        verify(projectRepository, never()).delete(any());
        verify(companyRepository, never()).save(any(Company.class));
        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    void existingCompanyWithoutProjectsReturnsZeros() {
        when(companyRepository.existsById(7L)).thenReturn(true);
        when(projectRepository.findCompanyProjectSummary(
                7L,
                ProjectStatus.DRAFT,
                ProjectStatus.AWAITING_REVIEWER,
                ProjectStatus.ACTIVE,
                ProjectStatus.COMPLETED,
                ProjectStatus.CANCELLED
        )).thenReturn(Optional.empty());

        var response = companyService.getProjectSummary(7L);

        assertEquals(0L, response.totalProjectCount());
        assertEquals(0L, response.draftProjectCount());
        assertEquals(0L, response.awaitingReviewerProjectCount());
        assertEquals(0L, response.activeProjectCount());
        assertEquals(0L, response.completedProjectCount());
        assertEquals(0L, response.cancelledProjectCount());
    }

    @Test
    void missingCompanyReturnsNotFoundWithoutProjectAggregation() {
        when(companyRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> companyService.getProjectSummary(99L)
        );

        verify(projectRepository, never()).findCompanyProjectSummary(
                any(), any(), any(), any(), any(), any()
        );
    }
}
