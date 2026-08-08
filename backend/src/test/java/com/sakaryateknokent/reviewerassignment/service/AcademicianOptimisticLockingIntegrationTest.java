package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.AcademicianImportResultResponse;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.importing.AcademicianImportRow;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.OptimisticLockException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
class AcademicianOptimisticLockingIntegrationTest {

    private static final String EMAIL_DOMAIN = "@optimistic-lock.test";

    private final AcademicianRepository academicianRepository;
    private final EntityManagerFactory entityManagerFactory;
    private final AcademicianImportService importService;

    private String runId;
    private String firstEmail;
    private String secondEmail;
    private String versionEmail;
    private String staleEmail;

    @Autowired
    AcademicianOptimisticLockingIntegrationTest(
            AcademicianRepository academicianRepository,
            EntityManagerFactory entityManagerFactory,
            AcademicianImportService importService
    ) {
        this.academicianRepository = academicianRepository;
        this.entityManagerFactory = entityManagerFactory;
        this.importService = importService;
    }

    @BeforeEach
    void setUp() {
        runId = UUID.randomUUID().toString().replace("-", "");
        firstEmail = "import-stale-" + runId + EMAIL_DOMAIN;
        secondEmail = "import-next-" + runId + EMAIL_DOMAIN;
        versionEmail = "version-" + runId + EMAIL_DOMAIN;
        staleEmail = "stale-" + runId + EMAIL_DOMAIN;
    }

    @AfterEach
    void cleanUp() {
        List.of(firstEmail, secondEmail, versionEmail, staleEmail)
                .forEach(email -> academicianRepository
                        .findByNormalizedEmail(email)
                        .forEach(academicianRepository::delete));
    }

    @Test
    void persistedAcademicianGetsVersionAndIncrementsOnUpdate() {
        Academician academician = academician(
                "Version",
                "Kontrol",
                versionEmail
        );

        Academician created = academicianRepository.saveAndFlush(academician);
        assertNotNull(created.getVersion());
        Long initialVersion = created.getVersion();

        created.setPhone("0555 000 00 00");
        Academician updated = academicianRepository.saveAndFlush(created);

        assertTrue(updated.getVersion() > initialVersion);
    }

    @Test
    void stalePersistenceContextUpdateFailsAndFirstCommitIsPreserved() {
        Academician academician = academician("Stale", "Kontrol", staleEmail);
        Long id = academicianRepository.saveAndFlush(academician).getId();

        EntityManager firstEntityManager =
                entityManagerFactory.createEntityManager();
        EntityManager secondEntityManager =
                entityManagerFactory.createEntityManager();
        EntityTransaction firstTransaction = firstEntityManager.getTransaction();
        EntityTransaction secondTransaction = secondEntityManager.getTransaction();

        try {
            firstTransaction.begin();
            secondTransaction.begin();

            Academician firstSnapshot =
                    firstEntityManager.find(Academician.class, id);
            Academician secondSnapshot =
                    secondEntityManager.find(Academician.class, id);

            firstSnapshot.setPhone("A");
            firstTransaction.commit();

            secondSnapshot.setPhone("B");
            assertThrows(OptimisticLockException.class, secondEntityManager::flush);
            secondTransaction.rollback();
        } finally {
            if (firstTransaction.isActive()) {
                firstTransaction.rollback();
            }
            if (secondTransaction.isActive()) {
                secondTransaction.rollback();
            }
            firstEntityManager.close();
            secondEntityManager.close();
        }

        Academician reloaded = academicianRepository.findById(id).orElseThrow();
        assertEquals("A", reloaded.getPhone());
    }

    @Test
    void importNeverUpdatesExistingRows() {
        academicianRepository.saveAndFlush(academician(
                "ImportStale",
                runId,
                firstEmail
        ));
        academicianRepository.saveAndFlush(academician(
                "ImportNext",
                runId,
                secondEmail
        ));

        AcademicianImportResultResponse result =
                importService.importCreateOnly(workbook());

        assertEquals(0, result.createdCount());
        assertEquals(0, result.updatedCount());
        assertEquals(2, result.conflictCount());
        assertEquals(0, result.failedCount());

        Academician first = academicianRepository
                .findByNormalizedEmail(firstEmail)
                .getFirst();
        Academician second = academicianRepository
                .findByNormalizedEmail(secondEmail)
                .getFirst();

        assertEquals(null, first.getPhone());
        assertEquals(null, second.getPhone());
    }

    private Academician academician(
            String firstName,
            String lastName,
            String email
    ) {
        Academician academician = new Academician();
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setEmail(email);
        return academician;
    }

    private MockMultipartFile workbook() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Akademisyenler");
            writeRow(sheet, 0, "AD", "SOYAD", "EMAIL", "TELEFON");
            writeRow(sheet, 1, "ImportStale", runId, firstEmail, "0555 111 11 11");
            writeRow(sheet, 2, "ImportNext", runId, secondEmail, "0666 222 22 22");
            workbook.write(outputStream);

            return new MockMultipartFile(
                    "file",
                    "optimistic-import.xlsx",
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

}
