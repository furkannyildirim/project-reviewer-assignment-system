package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.CompanyProjectSummaryResponse;
import com.sakaryateknokent.reviewerassignment.exception.GlobalExceptionHandler;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.service.CompanyExportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyProjectSummaryControllerTest {

    private final CompanyService companyService = mock(CompanyService.class);
    private final CompanyController companyController = new CompanyController(
            companyService,
            mock(CompanyExportService.class),
            mock(CompanyImportPreviewService.class),
            mock(CompanyImportService.class)
    );

    @Test
    void projectSummaryReturnsOnlySafeCountFields() throws Exception {
        when(companyService.getProjectSummary(7L)).thenReturn(
                new CompanyProjectSummaryResponse(7L, 9, 1, 2, 3, 2, 1)
        );

        mockMvc().perform(get("/api/companies/7/project-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyId").value(7))
                .andExpect(jsonPath("$.totalProjectCount").value(9))
                .andExpect(jsonPath("$.draftProjectCount").value(1))
                .andExpect(jsonPath("$.awaitingReviewerProjectCount").value(2))
                .andExpect(jsonPath("$.activeProjectCount").value(3))
                .andExpect(jsonPath("$.completedProjectCount").value(2))
                .andExpect(jsonPath("$.cancelledProjectCount").value(1))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.projects").doesNotExist());
    }

    @Test
    void projectSummaryReturnsNotFoundForMissingCompany() throws Exception {
        when(companyService.getProjectSummary(99L)).thenThrow(
                new ResourceNotFoundException("Sirket bulunamadi: 99")
        );

        mockMvc().perform(get("/api/companies/99/project-summary"))
                .andExpect(status().isNotFound());
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(companyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
