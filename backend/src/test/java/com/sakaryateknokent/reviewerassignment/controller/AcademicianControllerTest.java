package com.sakaryateknokent.reviewerassignment.controller;

import com.sakaryateknokent.reviewerassignment.service.AcademicianImportPreviewService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportService;
import com.sakaryateknokent.reviewerassignment.service.AcademicianService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AcademicianControllerTest {

    private final AcademicianService academicianService =
            mock(AcademicianService.class);
    private final AcademicianController academicianController =
            new AcademicianController(
                    academicianService,
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
}
