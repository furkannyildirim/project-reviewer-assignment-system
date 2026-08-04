package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.service.AcademicianImportClassifier.AcademicianImportDecision;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
class AcademicianImportPartialIntegrationTest {

    private static final String EMAIL_DOMAIN = "@partial-import.test";

    private final AcademicianImportService importService;
    private final AcademicianRepository academicianRepository;
    private final RaceInjectingPersistenceService persistenceService;

    private String runId;
    private String rowOneEmail;
    private String failedRowEmail;
    private String rowThreeEmail;

    @Autowired
    AcademicianImportPartialIntegrationTest(
            AcademicianImportService importService,
            AcademicianRepository academicianRepository,
            RaceInjectingPersistenceService persistenceService
    ) {
        this.importService = importService;
        this.academicianRepository = academicianRepository;
        this.persistenceService = persistenceService;
    }

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString().replace("-", "");
        rowOneEmail = "row1-" + runId + EMAIL_DOMAIN;
        failedRowEmail = "row2-" + runId + EMAIL_DOMAIN;
        rowThreeEmail = "row3-" + runId + EMAIL_DOMAIN;
        persistenceService.arm(failedRowEmail);
    }

    @AfterEach
    void cleanUp() {
        List.of(rowOneEmail, failedRowEmail, rowThreeEmail)
                .forEach(email -> academicianRepository
                        .findByNormalizedEmail(email)
                        .forEach(academicianRepository::delete));
    }

    @Test
    void oneDatabaseFailureDoesNotRollbackSuccessfulRows() {
        AcademicianImportResultResponse result =
                importService.importCreateOnly(workbook());

        assertEquals(2, result.createdCount());
        assertEquals(1, result.failedCount());
        assertEquals("FAILED", result.failedRows().getFirst().status());

        assertTrue(hasAcademician(rowOneEmail, "PartialRowOne"));
        assertFalse(hasAcademician(failedRowEmail, "PartialRowTwo"));
        assertTrue(hasAcademician(rowThreeEmail, "PartialRowThree"));
    }

    private boolean hasAcademician(String email, String firstName) {
        return academicianRepository.findByNormalizedEmail(email).stream()
                .anyMatch(academician -> firstName.equals(
                        academician.getFirstName()
                ));
    }

    private MockMultipartFile workbook() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL");
            writeRow(sheet, 1, "PartialRowOne", runId, rowOneEmail);
            writeRow(sheet, 2, "PartialRowTwo", runId, failedRowEmail);
            writeRow(sheet, 3, "PartialRowThree", runId, rowThreeEmail);
            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    "partial-import.xlsx",
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

    @TestConfiguration
    static class PartialImportTestConfiguration {

        @Bean
        @Primary
        RaceInjectingPersistenceService raceInjectingPersistenceService(
                AcademicianRepository academicianRepository,
                DataSource dataSource,
                @Qualifier("academicianImportPersistenceService")
                AcademicianImportPersistenceService delegate
        ) {
            return new RaceInjectingPersistenceService(
                    academicianRepository,
                    dataSource,
                    delegate
            );
        }
    }

    static class RaceInjectingPersistenceService
            extends AcademicianImportPersistenceService {

        private final DataSource dataSource;
        private final AcademicianImportPersistenceService delegate;
        private String emailToRace;

        RaceInjectingPersistenceService(
                AcademicianRepository academicianRepository,
                DataSource dataSource,
                AcademicianImportPersistenceService delegate
        ) {
            super(academicianRepository);
            this.dataSource = dataSource;
            this.delegate = delegate;
        }

        void arm(String emailToRace) {
            this.emailToRace = emailToRace;
        }

        @Override
        public void create(AcademicianImportRow row) {
            if (row.getEmail() != null && row.getEmail().equals(emailToRace)) {
                insertRaceRow(row);
            }

            delegate.create(row);
        }

        @Override
        public void update(AcademicianImportDecision decision) {
            delegate.update(decision);
        }

        private void insertRaceRow(AcademicianImportRow row) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         """
                                 INSERT INTO academicians
                                     (first_name,
                                      last_name,
                                      email,
                                      phone,
                                      title,
                                      canonical_first_name,
                                      canonical_last_name,
                                      canonical_title)
                                 VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                 """
                 )) {
                connection.setAutoCommit(true);
                statement.setString(1, "RaceHolder");
                statement.setString(2, row.getLastName());
                statement.setString(3, row.getEmail());
                statement.setString(4, null);
                statement.setString(5, null);
                statement.setString(6, AcademicianTextNormalizer
                        .canonicalizeIdentityPart("RaceHolder"));
                statement.setString(7, AcademicianTextNormalizer
                        .canonicalizeIdentityPart(row.getLastName()));
                statement.setString(8, "");
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
