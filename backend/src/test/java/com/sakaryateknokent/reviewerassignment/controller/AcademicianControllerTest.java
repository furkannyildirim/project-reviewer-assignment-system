package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianResponse;
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
