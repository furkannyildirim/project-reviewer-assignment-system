package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianExcelImportParser;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianIdentityProjection;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicianImportServiceTest {

    private final AcademicianRepository academicianRepository =
            mock(AcademicianRepository.class);
    private final AcademicianImportService importService =
            new AcademicianImportService(
                    new AcademicianExcelImportParser(),
                    academicianRepository
            );

    @Test
    void createsValidAcademicians() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of());

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "AYSE@EXAMPLE.COM");
                    writeRow(sheet, 2, "Ali", "Demir", "ali@example.com");
                })
        );

        assertEquals(2, response.createdCount());
        assertEquals(0, response.skippedDuplicateCount());
        assertEquals("CREATED", response.createdRows().getFirst().status());
        assertEquals("ayse@example.com", savedAcademicians().getFirst().getEmail());
    }

    @Test
    void savesKeywordsWithEntity() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "ANAHTAR KELIMELER");
                    writeRow(sheet, 1, "Ayse", "Yilmaz",
                            "Yapay   Zeka, robotik; yapay zeka");
                })
        );

        assertEquals(1, response.createdCount());
        assertIterableEquals(
                List.of("Yapay Zeka", "robotik"),
                List.copyOf(savedAcademicians().getFirst().getKeywords())
        );
    }

    @Test
    void keepsBlankEmailAndPhoneAsNull() {
        importService.importCreateOnly(workbook("akademisyenler.xlsx", sheet -> {
            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL", "TELEFON");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "", "");
        }));

        Academician saved = savedAcademicians().getFirst();
        assertNull(saved.getEmail());
        assertNull(saved.getPhone());
    }

    @Test
    void skipsExistingEmail() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of("ayse@example.com"));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com");
                    writeRow(sheet, 2, "Ali", "Demir", "ali@example.com");
                })
        );

        assertEquals(1, response.createdCount());
        assertEquals(1, response.skippedDuplicateCount());
        assertEquals("SKIPPED_DUPLICATE", response.skippedRows().getFirst().status());
        assertEquals("ali@example.com", savedAcademicians().getFirst().getEmail());
    }

    @Test
    void skipsDuplicateEmailInsideFileAfterFirstCandidate() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of());

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "same@example.com");
                    writeRow(sheet, 2, "Aysel", "Yildiz", "SAME@example.com");
                })
        );

        assertEquals(1, response.createdCount());
        assertEquals(1, response.skippedDuplicateCount());
        assertEquals("Ayse", savedAcademicians().getFirst().getFirstName());
    }

    @Test
    void skipsExistingNoEmailIdentity() {
        when(academicianRepository.findIdentities())
                .thenReturn(List.of(identity("ayse", "yilmaz", "doc. dr.")));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN");
                    writeRow(sheet, 1, " Ayse ", "Yilmaz", "Doc.   Dr.");
                })
        );

        assertEquals(0, response.createdCount());
        assertEquals(1, response.skippedDuplicateCount());
    }

    @Test
    void skipsNoEmailRowWhenIdentityMatchesExistingAcademicianWithEmail() {
        when(academicianRepository.findIdentities())
                .thenReturn(List.of(identity("ayse", "yilmaz", "doc. dr.")));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN", "EMAIL");
                    writeRow(sheet, 1, " Ayse ", "Yilmaz", "Doc.   Dr.", "");
                })
        );

        assertEquals(0, response.createdCount());
        assertEquals(1, response.skippedDuplicateCount());
        assertEquals("SKIPPED_DUPLICATE", response.skippedRows().getFirst().status());
        verify(academicianRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void skipsDuplicateNoEmailIdentityInsideFileAfterFirstCandidate() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "UNVAN");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "Doc. Dr.");
                    writeRow(sheet, 2, " ayse ", " yilmaz ", "Doc.   Dr.");
                })
        );

        assertEquals(1, response.createdCount());
        assertEquals(1, response.skippedDuplicateCount());
        assertEquals(1, savedAcademicians().size());
    }

    @Test
    void skipsAllRowsWhenSameFileIsImportedAgain() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of("ayse@example.com"));
        when(academicianRepository.findIdentities())
                .thenReturn(List.of(identity("ali", "demir", "")));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL", "UNVAN");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com", "");
                    writeRow(sheet, 2, "Ali", "Demir", "", "");
                })
        );

        assertEquals(0, response.createdCount());
        assertEquals(2, response.skippedDuplicateCount());
    }

    @Test
    void createsValidRowsWithCombinedNameGeneralWarning() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "ADI SOYADI");
                    writeRow(sheet, 1, "Dr. Ayse Yilmaz");
                })
        );

        assertEquals(1, response.createdCount());
        assertEquals(1, response.warningCount());
        assertTrue(response.generalWarnings().contains(
                "Birleşik ad soyad alanları otomatik ayrıştırılmıştır. "
                        + "Gerekirse aktarımdan sonra düzenleyebilirsiniz."
        ));
    }

    @Test
    void doesNotSaveWhenAllRowsAreInvalid() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> importService.importCreateOnly(
                        workbook("akademisyenler.xlsx", sheet -> {
                            writeRow(sheet, 0, "AD", "SOYAD");
                            writeRow(sheet, 1, "Ayse", "");
                        })
                )
        );

        assertEquals(
                "Dosyada aktarılabilir geçerli kayıt bulunamadı",
                exception.getMessage()
        );
        verify(academicianRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void savesOnlyValidRowsWhenValidAndInvalidRowsAreMixed() {
        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD");
                    writeRow(sheet, 1, "Ayse", "Yilmaz");
                    writeRow(sheet, 2, "Ali", "");
                })
        );

        assertEquals(2, response.totalDetectedRows());
        assertEquals(1, response.createdCount());
        assertEquals(0, response.skippedDuplicateCount());
        assertEquals(1, response.skippedInvalidCount());
        assertEquals("SKIPPED_INVALID", response.invalidRows().getFirst().status());
        assertEquals("Ayse", savedAcademicians().getFirst().getFirstName());
    }

    @Test
    void keepsInvalidRowsOutOfSaveAllCandidateList() {
        importService.importCreateOnly(workbook("akademisyenler.xlsx", sheet -> {
            writeRow(sheet, 0, "AD", "SOYAD");
            writeRow(sheet, 1, "Ayse", "Yilmaz");
            writeRow(sheet, 2, "Ali", "");
        }));

        List<Academician> saved = savedAcademicians();
        assertEquals(1, saved.size());
        assertEquals("Ayse", saved.getFirst().getFirstName());
    }

    @Test
    void classifiesValidDuplicateAndInvalidRowsSeparately() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of("existing@example.com"));

        AcademicianImportResultResponse response = importService.importCreateOnly(
                workbook("akademisyenler.xlsx", sheet -> {
                    writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                    writeRow(sheet, 1, "Ayse", "Yilmaz", "new@example.com");
                    writeRow(sheet, 2, "Mehmet", "Demir", "existing@example.com");
                    writeRow(sheet, 3, "", "Eksik", "");
                })
        );

        assertEquals(3, response.totalDetectedRows());
        assertEquals(1, response.createdCount());
        assertEquals(1, response.skippedDuplicateCount());
        assertEquals(1, response.skippedInvalidCount());
        assertEquals("SKIPPED_DUPLICATE", response.skippedRows().getFirst().status());
        assertEquals("SKIPPED_INVALID", response.invalidRows().getFirst().status());
        assertEquals(1, savedAcademicians().size());
    }

    @Test
    void performsDuplicateChecksWithBulkQueriesOnly() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of());
        when(academicianRepository.findIdentities())
                .thenReturn(List.of());

        importService.importCreateOnly(workbook("akademisyenler.xlsx", sheet -> {
            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
            writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com");
            writeRow(sheet, 2, "Ali", "Demir", "ali@example.com");
            writeRow(sheet, 3, "Zeynep", "Ak", "");
        }));

        verify(academicianRepository, times(1))
                .findExistingEmails(anyCollection());
        verify(academicianRepository, times(1))
                .findIdentities();
        verify(academicianRepository, never()).existsByEmail(any());
    }

    @Test
    void returnsSafeExceptionWhenSaveFails() {
        when(academicianRepository.findExistingEmails(anyCollection()))
                .thenReturn(Set.of());
        when(academicianRepository.saveAllAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("constraint x"));

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> importService.importCreateOnly(
                        workbook("akademisyenler.xlsx", sheet -> {
                            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
                            writeRow(sheet, 1, "Ayse", "Yilmaz", "ayse@example.com");
                        })
                )
        );

        assertFalse(exception.getMessage().contains("constraint"));
    }

    @SuppressWarnings("unchecked")
    private List<Academician> savedAcademicians() {
        ArgumentCaptor<Iterable<Academician>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(academicianRepository).saveAllAndFlush(captor.capture());

        List<Academician> academicians = new ArrayList<>();
        captor.getValue().forEach(academicians::add);
        return academicians;
    }

    private AcademicianIdentityProjection identity(
            String firstName,
            String lastName,
            String title
    ) {
        return new AcademicianIdentityProjection() {
            @Override
            public String getFirstName() {
                return firstName;
            }

            @Override
            public String getLastName() {
                return lastName;
            }

            @Override
            public String getTitle() {
                return title;
            }
        };
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
}
