package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyControllerTest {

    private final CompanyService companyService = mock(CompanyService.class);
    private final CompanyController companyController = new CompanyController(
            companyService,
            mock(CompanyImportPreviewService.class),
            mock(CompanyImportService.class)
    );

    @Test
    void countCompaniesReturnsTgbCompanyCount() {
        when(companyService.countCompanies(CompanyType.TGB)).thenReturn(120L);

        ResponseEntity<Long> response =
                companyController.countCompanies(CompanyType.TGB);

        assertEquals(120L, response.getBody());
        verify(companyService).countCompanies(CompanyType.TGB);
    }

    @Test
    void countCompaniesReturnsExternalCompanyCount() {
        when(companyService.countCompanies(CompanyType.EXTERNAL))
                .thenReturn(340L);

        ResponseEntity<Long> response =
                companyController.countCompanies(CompanyType.EXTERNAL);

        assertEquals(340L, response.getBody());
        verify(companyService).countCompanies(CompanyType.EXTERNAL);
    }
}
