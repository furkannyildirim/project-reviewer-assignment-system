package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
import com.sakaryateknokent.reviewerassignment.dto.AcademicianProjectSummaryResponse;
import com.sakaryateknokent.reviewerassignment.dto.ExportSelectionRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateAcademicianRequest;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.exception.GlobalExceptionHandler;
import com.sakaryateknokent.reviewerassignment.service.AcademicianExportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AcademicianControllerTest {

    private final AcademicianService academicianService =
            mock(AcademicianService.class);
    private final AcademicianExportService academicianExportService =
            mock(AcademicianExportService.class);
    private final AcademicianController academicianController =
            new AcademicianController(
                    academicianService,
                    academicianExportService,
                    mock(AcademicianImportPreviewService.class),
                    mock(AcademicianImportService.class)
            );

    @Test
    void projectSummaryReturnsOnlySafeCountFields() throws Exception {
        when(academicianService.getProjectSummary(7L)).thenReturn(
                new AcademicianProjectSummaryResponse(7L, 2, 3, 5)
        );
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(academicianController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/academicians/7/project-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academicianId").value(7))
                .andExpect(jsonPath("$.activeProjectCount").value(2))
                .andExpect(jsonPath("$.completedProjectCount").value(3))
                .andExpect(jsonPath("$.totalProjectCount").value(5))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.assignments").doesNotExist());
    }

    @Test
    void projectSummaryReturnsNotFoundForMissingAcademician() throws Exception {
        when(academicianService.getProjectSummary(99L)).thenThrow(
                new com.sakaryateknokent.reviewerassignment.exception
                        .ResourceNotFoundException("Akademisyen bulunamadı: 99")
        );
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(academicianController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/academicians/99/project-summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void countAcademiciansReturnsTotalAcademicianCount() {
        when(academicianService.countAcademicians()).thenReturn(248L);

        ResponseEntity<Long> response =
                academicianController.countAcademicians();

        assertEquals(248L, response.getBody());
        verify(academicianService).countAcademicians();
    }

    @Test
    void exportAcademiciansReturnsXlsxAttachment() {
        List<AcademicianResponse> academicians = List.of(
                new AcademicianResponse(
                        1L,
                        0L,
                        "Ayse",
                        "Yilmaz",
                        null,
                        null,
                        null,
                        null
                )
        );
        byte[] fileContent = new byte[]{1, 2, 3};
        when(academicianService.getAcademiciansForExport(" yapay ", " A "))
                .thenReturn(academicians);
        when(academicianExportService.exportToExcel(academicians))
                .thenReturn(fileContent);

        ResponseEntity<byte[]> response =
                academicianController.exportAcademicians(" yapay ", " A ");

        assertArrayEquals(fileContent, response.getBody());
        assertEquals(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getHeaders().getContentType().toString()
        );
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("attachment"));
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("akademisyenler.xlsx"));
        verify(academicianService).getAcademiciansForExport(" yapay ", " A ");
        verify(academicianExportService).exportToExcel(academicians);
    }

    @Test
    void getAcademicianIdsReturnsFilteredIds() {
        when(academicianService.getAcademicianIds("yazilim", "A"))
                .thenReturn(List.of(1L, 2L));

        ResponseEntity<List<Long>> response =
                academicianController.getAcademicianIds("yazilim", "A");

        assertEquals(List.of(1L, 2L), response.getBody());
        verify(academicianService).getAcademicianIds("yazilim", "A");
    }

    @Test
    void exportSelectedAcademiciansUsesSelectedIds() {
        List<AcademicianResponse> academicians = List.of(
                new AcademicianResponse(
                        1L,
                        0L,
                        "Ayse",
                        "Yilmaz",
                        null,
                        null,
                        null,
                        null
                )
        );
        byte[] fileContent = new byte[]{7};
        when(academicianService.getAcademiciansForExport(List.of(1L, 1L)))
                .thenReturn(academicians);
        when(academicianExportService.exportToExcel(academicians))
                .thenReturn(fileContent);

        ResponseEntity<byte[]> response =
                academicianController.exportSelectedAcademicians(
                        new ExportSelectionRequest(List.of(1L, 1L))
                );

        assertArrayEquals(fileContent, response.getBody());
        assertTrue(response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION)
                .contains("akademisyenler.xlsx"));
        verify(academicianService).getAcademiciansForExport(List.of(1L, 1L));
        verify(academicianExportService).exportToExcel(academicians);
    }

    @Test
    void staleUpdateReturnsConflictWithoutRawException() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(academicianController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        doThrow(new ObjectOptimisticLockingFailureException(
                Academician.class,
                9L
        )).when(academicianService).updateAcademician(
                eq(9L),
                any(UpdateAcademicianRequest.class)
        );

        mockMvc.perform(put("/api/academicians/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1,
                                  "firstName": "Ayse",
                                  "lastName": "Yilmaz",
                                  "email": "ayse@example.com",
                                  "phone": null,
                                  "title": null,
                                  "keywords": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Akademisyen kaydı başka bir işlem tarafından güncellendi. "
                                + "Lütfen sayfayı yenileyip tekrar deneyin."
                ))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers
                        .not(org.hamcrest.Matchers.containsString("version"))));
    }
}
