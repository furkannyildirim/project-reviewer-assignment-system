package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.ProjectUpload;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectUploadStatus;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectUploadRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
class ProjectUploadPessimisticLockIntegrationTest {

    private final ProjectCreationFromUploadService creationService;
    private final ProjectUploadRepository uploadRepository;
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @MockitoBean
    private ProjectFileStorageService fileStorageService;

    private UUID uploadId;
    private Long companyId;
    private Long userId;
    private String projectName;

    @Autowired
    ProjectUploadPessimisticLockIntegrationTest(
            ProjectCreationFromUploadService creationService,
            ProjectUploadRepository uploadRepository,
            ProjectRepository projectRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository
    ) {
        this.creationService = creationService;
        this.uploadRepository = uploadRepository;
        this.projectRepository = projectRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    @AfterEach
    void cleanUp() {
        if (projectName != null) {
            List<Project> projects = projectRepository.findAll().stream()
                    .filter(project -> projectName.equals(project.getName()))
                    .toList();
            projectRepository.deleteAll(projects);
        }
        if (uploadId != null) {
            uploadRepository.deleteById(uploadId);
        }
        if (companyId != null) {
            companyRepository.deleteById(companyId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void sameUploadIsConsumedByOnlyOneConcurrentProjectCreation() throws Exception {
        String runId = UUID.randomUUID().toString();
        String email = "upload-lock-" + runId + "@example.com";
        projectName = "Upload lock " + runId;
        User user = userRepository.saveAndFlush(user(email));
        userId = user.getId();
        Company company = companyRepository.saveAndFlush(company(runId));
        companyId = company.getId();
        uploadId = UUID.randomUUID();
        String temporaryKey = "temporary/" + UUID.randomUUID() + ".pdf";
        uploadRepository.saveAndFlush(upload(uploadId, user, temporaryKey));

        CountDownLatch firstCopyStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstCopy = new CountDownLatch(1);
        CountDownLatch secondRequestStarted = new CountDownLatch(1);
        CountDownLatch secondCopyStarted = new CountDownLatch(1);
        AtomicInteger copyCount = new AtomicInteger();
        StoredProjectFile permanentFile = new StoredProjectFile(
                UUID.randomUUID(), "projects/" + UUID.randomUUID() + ".pdf",
                "project.pdf", "application/pdf", 10);
        when(fileStorageService.copyTemporaryToProjects(any())).thenAnswer(invocation -> {
            if (copyCount.incrementAndGet() == 1) {
                firstCopyStarted.countDown();
                assertTrue(releaseFirstCopy.await(10, TimeUnit.SECONDS));
            } else {
                secondCopyStarted.countDown();
            }
            return permanentFile;
        });

        CreateProjectFromUploadRequest request = request(uploadId, companyId, projectName);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Attempt> first = executor.submit(() -> attemptCreate(request, email));
            assertTrue(firstCopyStarted.await(10, TimeUnit.SECONDS));
            Future<Attempt> second = executor.submit(() -> {
                secondRequestStarted.countDown();
                return attemptCreate(request, email);
            });
            assertTrue(secondRequestStarted.await(10, TimeUnit.SECONDS));

            assertFalse(secondCopyStarted.await(500, TimeUnit.MILLISECONDS));
            releaseFirstCopy.countDown();

            List<Attempt> attempts = List.of(
                    first.get(10, TimeUnit.SECONDS),
                    second.get(10, TimeUnit.SECONDS)
            );
            assertEquals(1, attempts.stream().filter(Attempt::successful).count());
            Throwable failure = attempts.stream()
                    .filter(attempt -> !attempt.successful())
                    .map(Attempt::failure)
                    .findFirst()
                    .orElseThrow();
            assertInstanceOf(InvalidRequestException.class, failure);
        } finally {
            releaseFirstCopy.countDown();
        }

        assertEquals(1, copyCount.get());
        assertEquals(1, projectRepository.findAll().stream()
                .filter(project -> projectName.equals(project.getName()))
                .count());
        ProjectUpload consumedUpload = uploadRepository.findById(uploadId).orElseThrow();
        assertEquals(ProjectUploadStatus.CONSUMED, consumedUpload.getStatus());
        verify(fileStorageService).deleteTemporary(temporaryKey);
        verify(fileStorageService, never()).deleteProjectFile(any());
    }

    private Attempt attemptCreate(CreateProjectFromUploadRequest request, String email) {
        try {
            return new Attempt(creationService.create(request, email), null);
        } catch (Throwable throwable) {
            return new Attempt(null, throwable);
        }
    }

    private User user(String email) {
        User user = new User();
        user.setFirstName("Upload");
        user.setLastName("Lock");
        user.setEmail(email);
        user.setPhone("5550000000");
        user.setPasswordHash("hash");
        return user;
    }

    private Company company(String runId) {
        Company company = new Company();
        company.setName("Upload Lock " + runId);
        company.setCompanyType(CompanyType.TGB);
        return company;
    }

    private ProjectUpload upload(UUID id, User user, String storageKey) {
        ProjectUpload upload = new ProjectUpload();
        upload.setId(id);
        upload.setUploadedBy(user);
        upload.setStorageKey(storageKey);
        upload.setOriginalFileName("project.pdf");
        upload.setContentType("application/pdf");
        upload.setFileSize(10L);
        upload.setStatus(ProjectUploadStatus.PENDING);
        upload.setExpiresAt(Instant.now().plusSeconds(300));
        upload.setCreatedAt(Instant.now());
        return upload;
    }

    private CreateProjectFromUploadRequest request(
            UUID id,
            Long requestedCompanyId,
            String name
    ) {
        return new CreateProjectFromUploadRequest(id, requestedCompanyId, name,
                null, LocalDate.of(2026, 1, 1), 3, null, null, null, null,
                null, null, null, Set.of());
    }

    private record Attempt(ProjectResponse response, Throwable failure) {
        private boolean successful() {
            return response != null;
        }
    }
}
