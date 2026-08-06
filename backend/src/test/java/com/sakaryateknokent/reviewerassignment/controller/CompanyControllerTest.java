package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.dto.ExportSelectionRequest;
import com.sakaryateknokent.reviewerassignment.service.CompanyExportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.CompanyImportService;
import com.sakaryateknokent.reviewerassignment.service.CompanyService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyControllerTest {

    private final CompanyService companyService = mock(CompanyService.class);
    private final CompanyExportService companyExportService =
            mock(CompanyExportService.class);
    private final CompanyController companyController = new CompanyController(
            companyService,
            companyExportService,
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

    @Test
    void exportCompaniesReturnsTgbExcelResponseWithFilename() {
        byte[] content = new byte[]{1, 2, 3};
        when(companyExportService.exportToExcel(
                CompanyType.TGB,
                "yazilim",
                "A"
        )).thenReturn(content);

        ResponseEntity<byte[]> response = companyController.exportCompanies(
                CompanyType.TGB,
                "yazilim",
                "A"
        );

        assertEquals(content, response.getBody());
        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString()
        );
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("tgb-sirketleri.xlsx"));
        verify(companyExportService).exportToExcel(
                CompanyType.TGB,
                "yazilim",
                "A"
        );
    }

    @Test
    void exportCompaniesReturnsExternalExcelResponseWithFilename() {
        when(companyExportService.exportToExcel(
                CompanyType.EXTERNAL,
                "",
                null
        )).thenReturn(new byte[]{4});

        ResponseEntity<byte[]> response = companyController.exportCompanies(
                CompanyType.EXTERNAL,
                "",
                null
        );

        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("dis-sirketler.xlsx"));
        verify(companyExportService).exportToExcel(
                CompanyType.EXTERNAL,
                "",
                null
        );
    }

    @Test
    void getCompanyIdsReturnsFilteredIds() {
        when(companyService.getCompanyIds(CompanyType.TGB, "yazilim", "D"))
                .thenReturn(java.util.List.of(1L, 2L));

        ResponseEntity<java.util.List<Long>> response =
                companyController.getCompanyIds(
                        CompanyType.TGB,
                        "yazilim",
                        "D"
                );

        assertEquals(java.util.List.of(1L, 2L), response.getBody());
        verify(companyService).getCompanyIds(CompanyType.TGB, "yazilim", "D");
    }

    @Test
    void exportSelectedCompaniesUsesSelectedIdsAndCompanyType() {
        byte[] content = new byte[]{9};
        when(companyExportService.exportToExcel(
                CompanyType.TGB,
                java.util.List.of(1L, 2L, 1L)
        )).thenReturn(content);

        ResponseEntity<byte[]> response =
                companyController.exportSelectedCompanies(
                        CompanyType.TGB,
                        new ExportSelectionRequest(
                                java.util.List.of(1L, 2L, 1L)
                        )
                );

        assertEquals(content, response.getBody());
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("tgb-sirketleri.xlsx"));
        verify(companyExportService).exportToExcel(
                CompanyType.TGB,
                java.util.List.of(1L, 2L, 1L)
        );
    }
}
