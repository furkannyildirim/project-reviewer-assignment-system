package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicianImportServiceTest {

    private final AcademicianRepository academicianRepository =
            mock(AcademicianRepository.class);
    private final AcademicianImportClassifier classifier =
            new AcademicianImportClassifier(academicianRepository);
    private final AcademicianImportPersistenceService persistenceService =
            new AcademicianImportPersistenceService(academicianRepository);
    private final AcademicianImportService importService =
            new AcademicianImportService(
                    new AcademicianExcelImportParser(),
                    classifier,
                    persistenceService
            );

    @Test
    void createsValidAcademiciansWhenDatabaseHasNoMatch() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "AYSE@EXAMPLE.COM");
                    writeRow(sheet, 2, "Ali", "Demir", "ali@example.com");
                    writeRow(sheet, 3, "Veli", "Kaya", "veli@example.com");
                })
        );

        assertEquals(3, response.createdCount());
        assertEquals(0, response.updatedCount());
        assertEquals(0, response.failedCount());
        assertEquals("CREATED", response.createdRows().getFirst().status());
        assertEquals("ayse@example.com", savedAcademicians().getFirst().getEmail());
    }

    @Test
    void importsValidSheetsWhenAnotherVisibleSheetHasInvalidStructure() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbookWithSheets("akademisyenler.xlsx", workbook -> {
                    Sheet first = workbook.createSheet("Sheet 1");
                    writeRow(first, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(first, 1, "Ayse", "Yilmaz", "ayse@example.com");

                    Sheet invalid = workbook.createSheet("Sheet 2");
                    writeRow(invalid, 0, "EMAIL", "TELEFON");
                    writeRow(invalid, 1, "skip@example.com", "0555");

                    Sheet third = workbook.createSheet("Sheet 3");
                    writeRow(third, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(third, 1, "Ali", "Demir", "ali@example.com");
                })
        );

        assertEquals(2, response.createdCount());
        assertEquals(0, response.invalidCount());
        assertEquals(1, response.warningCount());
        assertEquals(1, response.generalWarnings().size());
        assertTrue(response.generalWarnings().getFirst().contains("Sheet 2"));
        assertIterableEquals(
                List.of("Sheet 1", "Sheet 3"),
                response.createdRows().stream()
                        .map(row -> row.sheetName())
                        .toList()
        );
    }

    @Test
    void persistenceOperationsUseRequiresNewTransactions()
            throws NoSuchMethodException {
        Method createMethod = AcademicianImportPersistenceService.class
                .getMethod("create",
                        com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow.class);

        assertEquals(
                Propagation.REQUIRES_NEW,
                createMethod.getAnnotation(Transactional.class).propagation()
        );
    }

    @Test
    void previewImportStatusesDoNotIncludeFailed() {
        assertTrue(Arrays.stream(AcademicianImportClassifier.ImportStatus.values())
                .noneMatch(status -> status.name().equals("FAILED")));
    }

    @Test
    void canonicalIdentityWithEmailOnOnlyOneSideRequiresManualReview() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        when(academicianRepository.findByNormalizedEmail("ayse@example.com"))
                .thenReturn(List.of());
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "ayse@example.com");
                })
        );

        assertEquals(1, response.conflictCount());
        assertEquals(0, response.updatedCount());
        assertTrue(response.conflictRows().getFirst().messages().getFirst()
                .contains("Otomatik güncelleme yapılmadı"));
        assertNull(existing.getEmail());
        verify(academicianRepository, never())
                .saveAndFlush(any(Academician.class));
    }

    @Test
    void additionalPhoneRequiresManualReviewWithoutUpdate() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "0532 111 22 33");
                })
        );

        assertEquals(1, response.conflictCount());
        assertEquals(0, response.updatedCount());
        assertNull(existing.getPhone());
        verify(academicianRepository, never())
                .saveAndFlush(any(Academician.class));
    }

    @Test
    void missingIncomingEmailRequiresManualReviewWithoutClearingValues() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        existing.setEmail("ayse@example.com");
        existing.setPhone("0532 111 22 33");
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.", "", "");
                })
        );

        assertEquals(1, response.conflictCount());
        assertEquals("ayse@example.com", existing.getEmail());
        assertEquals("0532 111 22 33", existing.getPhone());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void unicodeWhitespaceDoesNotSplitCanonicalIdentityDuringClassification() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN");
                    writeRow(sheet, 1, "Ayse\u00A0", "Yilmaz",
                            "Doc.\u00A0Dr.");
                })
        );

        assertEquals(1, response.duplicateCount());
        assertEquals(0, response.createdCount());
        assertEquals("DUPLICATE", response.duplicateRows().getFirst().status());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void importNewRefreshesPersistedCanonicalIdentity() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN");
                    writeRow(sheet, 1, "Mahir", "I\u015F\u0131k",
                            "Dr.\u00A0\u00D6\u011Fr.\u00A0\u00DCyesi");
                })
        );

        Academician saved = savedAcademicians().getFirst();
        assertEquals(1, response.createdCount());
        assertEquals("mahir", saved.getCanonicalFirstName());
        assertEquals("\u0131\u015F\u0131k", saved.getCanonicalLastName());
        assertEquals(
                "dr. \u00F6\u011Fr. \u00FCyesi",
                saved.getCanonicalTitle()
        );
    }

    @Test
    void sameExistingEmailAndNoNewInformationIsDuplicate() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        existing.setEmail("ayse@example.com");
        existing.setPhone("0532");
        when(academicianRepository.findByNormalizedEmail("ayse@example.com"))
                .thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "ayse@example.com", "0532");
                })
        );

        assertEquals(1, response.duplicateCount());
        assertEquals(0, response.updatedCount());
        assertEquals("DUPLICATE", response.duplicateRows().getFirst().status());
    }

    @Test
    void sameCanonicalIdentityWithDifferentEmailsIsNew() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        existing.setEmail("old@example.com");
        when(academicianRepository.findByNormalizedEmail("new@example.com"))
                .thenReturn(List.of());
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "new@example.com");
                })
        );

        assertEquals(1, response.createdCount());
        assertEquals(0, response.conflictCount());
        assertEquals("old@example.com", existing.getEmail());
        assertEquals(
                "new@example.com",
                savedAcademicians().getFirst().getEmail()
        );
    }

    @Test
    void conflictingExistingPhoneIsNotOverwritten() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        existing.setPhone("0532");
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.", "0544");
                })
        );

        assertEquals(1, response.conflictCount());
        assertEquals("0532", existing.getPhone());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void sameEmailWithDifferentIdentityIsConflict() {
        Academician existing = academician("Baska", "Kisi", "Prof. Dr.");
        existing.setEmail("same@example.com");
        when(academicianRepository.findByNormalizedEmail("same@example.com"))
                .thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "same@example.com");
                })
        );

        assertEquals(1, response.conflictCount());
        assertTrue(response.conflictRows().getFirst().messages().getFirst()
                .contains("Otomatik güncelleme yapılmadı"));
    }

    @Test
    void multipleDatabaseIdentityMatchesAreConflict() {
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(
                academician("Ayse", "Yilmaz", "Doc. Dr."),
                academician("Ayse", "Yilmaz", "Doc. Dr.")
        ));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.");
                })
        );

        assertEquals(1, response.conflictCount());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void existingAcademicianWithAdditionalKeywordsRequiresManualReviewWithoutUpdate() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        existing.getKeywords().addAll(List.of("Java", "Spring"));
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "KEYWORDS");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "Spring; Yapay Zeka");
                })
        );

        assertEquals(1, response.conflictCount());
        assertEquals(0, response.updatedCount());
        assertIterableEquals(
                List.of("Java", "Spring"),
                List.copyOf(existing.getKeywords())
        );
        assertTrue(response.conflictRows().getFirst().messages().getFirst()
                .contains("Otomatik güncelleme yapılmadı"));
        verify(academicianRepository, never())
                .saveAndFlush(any(Academician.class));
    }

    @Test
    void sameFileCanonicalMatchWithEmailOnOnlyOneRowRequiresManualReview() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.", "", "");
                    writeRow(sheet, 2, " Ayse ", "Yilmaz", "Doc.   Dr.",
                            "ayse@example.com", "0532");
                })
        );

        assertEquals(0, response.createdCount());
        assertEquals(2, response.conflictCount());
        verify(academicianRepository, never())
                .saveAndFlush(any(Academician.class));
    }

    @Test
    void sameFileDifferentEmailsForSameIdentityAreSeparateNewAcademicians() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "a@example.com");
                    writeRow(sheet, 2, "Ayse", "Yilmaz", "b@example.com");
                })
        );

        assertEquals(2, response.createdCount());
        assertEquals(0, response.conflictCount());
        assertEquals(2, savedAcademicians().size());
    }

    @Test
    void sameFileDifferentPhonesForSameIdentityAreConflict() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "0532");
                    writeRow(sheet, 2, "Ayse", "Yilmaz", "0544");
                })
        );

        assertEquals(1, response.conflictCount());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void sameFileSameEmailWithDifferentIdentitiesIsConflict() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "same@example.com");
                    writeRow(sheet, 2, "Ali", "Demir", "same@example.com");
                })
        );

        assertEquals(1, response.conflictCount());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @Test
    void invalidRowsDoNotJoinSameFileMatchingAndPartialImportContinues() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com");
                    writeRow(sheet, 2, "", "Yilmaz", "other@example.com");
                })
        );

        assertEquals(1, response.createdCount());
        assertEquals(1, response.invalidCount());
        assertEquals("INVALID", response.invalidRows().getFirst().status());
        assertEquals(1, savedAcademicians().size());
    }

    @Test
    void communicationColumnAdditionalContactValuesRequireManualReview() {
        Academician existing = academician("Ayse", "Yilmaz", null);
        when(academicianRepository.findByNormalizedEmail("ayse@example.com"))
                .thenReturn(List.of());
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                ""
        )).thenReturn(List.of(existing));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "ILETISIM");
                    writeRow(sheet, 1, "Ayse", "Yilmaz",
                            "0532 111 22 33 / ayse@example.com");
                })
        );

        assertEquals(1, response.conflictCount());
        assertEquals(0, response.updatedCount());
        assertNull(existing.getEmail());
        assertNull(existing.getPhone());
        verify(academicianRepository, never())
                .saveAndFlush(any(Academician.class));
    }

    @Test
    void failedNewRowDoesNotStopFollowingRows() {
        doAnswer(invocation -> invocation.getArgument(0))
                .doThrow(new DataIntegrityViolationException("constraint x"))
                .doAnswer(invocation -> invocation.getArgument(0))
                .when(academicianRepository)
                .saveAndFlush(any(Academician.class));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com");
                    writeRow(sheet, 2, "Ali", "Demir", "ali@example.com");
                    writeRow(sheet, 3, "Veli", "Kaya", "veli@example.com");
                })
        );

        assertEquals(2, response.createdCount());
        assertEquals(1, response.failedCount());
        assertEquals(0, response.updatedCount());
        assertEquals("FAILED", response.failedRows().getFirst().status());
        assertTrue(response.failedRows().getFirst().messages().getFirst()
                .contains("Kayit veritabanina yazilirken hata olustu"));
        assertEquals(3, savedAcademicians().size());
    }

    @Test
    void manualReviewRowDoesNotStopFollowingNewRows() {
        Academician existing = academician("Ayse", "Yilmaz", "Doc. Dr.");
        when(academicianRepository.findByCanonicalIdentity(
                "ayse",
                "yilmaz",
                "doc. dr."
        )).thenReturn(List.of(existing));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(academicianRepository)
                .saveAndFlush(any(Academician.class));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.",
                            "ayse@example.com");
                    writeRow(sheet, 2, "Ali", "Demir", "Doc. Dr.",
                            "ali@example.com");
                })
        );

        assertEquals(0, response.updatedCount());
        assertEquals(1, response.createdCount());
        assertEquals(1, response.conflictCount());
        assertEquals(0, response.failedCount());
        verify(academicianRepository, atLeastOnce())
                .saveAndFlush(any(Academician.class));
    }

    @Test
    void existingRowsNeverInvokeImportPersistence() {
        AcademicianImportPersistenceService failingPersistenceService =
                mock(AcademicianImportPersistenceService.class);
        AcademicianImportService service = new AcademicianImportService(
                new AcademicianExcelImportParser(),
                classifier,
                failingPersistenceService
        );
        when(academicianRepository.findByNormalizedEmail("ayse@example.com"))
                .thenReturn(List.of(academician("Ayse", "Yilmaz", null)));
        when(academicianRepository.findByNormalizedEmail("ali@example.com"))
                .thenReturn(List.of(academician("Ali", "Demir", null)));
        AcademicianImportResultResponse response = service.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL", "TELEFON");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com",
                            "0555 111 11 11");
                    writeRow(sheet, 2, "Ali", "Demir", "ali@example.com",
                            "0666 222 22 22");
                })
        );

        assertEquals(0, response.updatedCount());
        assertEquals(2, response.conflictCount());
        assertEquals(0, response.failedCount());
        verify(failingPersistenceService, never())
                .create(any(com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow.class));
    }

    @Test
    void duplicateConflictAndInvalidRowsStillDoNotWriteToDatabase() {
        Academician duplicate = academician("Ali", "Demir", "Prof. Dr.");
        duplicate.setEmail("ali@example.com");
        Academician conflict = academician("Baska", "Kisi", "Prof. Dr.");
        conflict.setEmail("conflict@example.com");
        when(academicianRepository.findByNormalizedEmail("ali@example.com"))
                .thenReturn(List.of(duplicate));
        when(academicianRepository.findByNormalizedEmail("conflict@example.com"))
                .thenReturn(List.of(conflict));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, "Ali", "Demir", "Prof. Dr.",
                            "ali@example.com");
                    writeRow(sheet, 2, "Ayse", "Yilmaz", "Prof. Dr.",
                            "conflict@example.com");
                    writeRow(sheet, 3, "", "Eksik", "Prof. Dr.",
                            "invalid@example.com");
                })
        );

        assertEquals(1, response.duplicateCount());
        assertEquals(1, response.conflictCount());
        assertEquals(1, response.invalidCount());
        assertEquals(0, response.failedCount());
        verify(academicianRepository, never()).saveAndFlush(any(Academician.class));
    }

    @SuppressWarnings("unchecked")
    private List<Academician> savedAcademicians() {
        ArgumentCaptor<Academician> captor =
                ArgumentCaptor.forClass(Academician.class);
        verify(academicianRepository, atLeastOnce())
                .saveAndFlush(captor.capture());

        return new ArrayList<>(captor.getAllValues());
    }

    private Academician academician(
            String firstName,
            String lastName,
            String title
    ) {
        Academician academician = new Academician();
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setTitle(title);
        return academician;
    }

    private MockMultipartFile workbook(
            String fileName,
            WorkbookBuilder builder
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            builder.build(workbook.createSheet("Akademisyenler"));
            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private MockMultipartFile workbookWithSheets(
            String fileName,
            MultiSheetWorkbookBuilder builder
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            builder.build(workbook);
            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void writeRow(Sheet sheet, int rowIndex, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    @FunctionalInterface
    private interface WorkbookBuilder {
        void build(Sheet sheet);
    }

    @FunctionalInterface
    private interface MultiSheetWorkbookBuilder {
        void build(Workbook workbook);
    }
}
