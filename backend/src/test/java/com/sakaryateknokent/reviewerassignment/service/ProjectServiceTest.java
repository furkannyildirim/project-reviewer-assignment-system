package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectRequest;
import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentHistoryResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentResponse;
import com.sakaryateknokent.reviewerassignment.dto.ReviewerAssignmentRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.ProjectReviewerAssignment;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    private final ProjectRepository projectRepository = org.mockito.Mockito.mock(ProjectRepository.class);
    private final CompanyRepository companyRepository = org.mockito.Mockito.mock(CompanyRepository.class);
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final ProjectReviewerAssignmentRepository assignmentRepository =
            org.mockito.Mockito.mock(ProjectReviewerAssignmentRepository.class);
    private final AcademicianRepository academicianRepository =
            org.mockito.Mockito.mock(AcademicianRepository.class);
    private final ProjectFileStorageService fileStorageService =
            org.mockito.Mockito.mock(ProjectFileStorageService.class);
    private final ProjectService projectService = new ProjectService(
            projectRepository, companyRepository, userRepository, assignmentRepository,
            academicianRepository, fileStorageService);

    @Test
    void createsProjectUsingAuthenticatedUserAndDraftStatus() {
        Company company = company(3L);
        User user = user(7L, "Ayse", "Yilmaz");
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0);
            project.setId(9L);
            return project;
        });

        ProjectResponse response = projectService.createProject(createRequest(
                LocalDate.of(2026, 1, 10), 6, Set.of("ai")), "user@example.com");

        assertEquals(9L, response.id());
        assertEquals(7L, response.createdById());
        assertEquals("Ayse Yilmaz", response.createdByName());
        Project saved = savedProject();
        assertEquals(user, saved.getCreatedBy());
        assertEquals(ProjectStatus.DRAFT, saved.getStatus());
    }

    @Test
    void calculatesEndDateFromStartDateAndDuration() {
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company(3L)));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user(7L, "Ayse", "Yilmaz")));
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(i -> i.getArgument(0));

        ProjectResponse response = projectService.createProject(createRequest(
                LocalDate.of(2026, 1, 31), 1, Set.of()), "user@example.com");

        assertEquals(LocalDate.of(2026, 2, 28), response.endDate());
    }

    @Test
    void rejectsCreateWhenCompanyDoesNotExist() {
        when(companyRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.createProject(
                createRequest(LocalDate.now(), 1, Set.of()), "user@example.com"));
        verify(projectRepository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    void normalizesKeywordsByTrimmingRemovingBlankAndIgnoringCaseDuplicates() {
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company(3L)));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user(7L, "Ayse", "Yilmaz")));
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(i -> i.getArgument(0));

        ProjectResponse response = projectService.createProject(createRequest(
                LocalDate.now(), 3, new LinkedHashSet<>(Set.of(" AI ", "ai", " ", "Robotik "))),
                "user@example.com");

        assertEquals(Set.of("AI", "Robotik"), response.keywords());
    }

    @Test
    void createsFromUploadWithAwaitingReviewerStatusVerifiedMetadataAndNormalizedKeywords() {
        Company company = company(3L);
        User user = user(7L, "Ayse", "Yilmaz");
        StoredProjectFile storedFile = new StoredProjectFile(UUID.randomUUID(),
                "projects/11111111-1111-1111-1111-111111111111.pdf",
                "onayli-kaynak.pdf", "application/pdf", 2048);
        when(projectRepository.saveAndFlush(any(Project.class))).thenAnswer(i -> {
            Project project = i.getArgument(0);
            project.setId(15L);
            return project;
        });

        ProjectResponse response = projectService.createProjectFromUpload(
                new CreateProjectFromUploadRequest(UUID.randomUUID(), company.getId(),
                        " Onayli Proje ", null, LocalDate.of(2026, 1, 31), 1,
                        null, null, null, null, null, null, null,
                        new LinkedHashSet<>(Set.of(" AI ", "ai", " ", "Robotik "))),
                company, user, storedFile);

        Project saved = savedProject();
        assertEquals(ProjectStatus.AWAITING_REVIEWER, saved.getStatus());
        assertEquals(LocalDate.of(2026, 2, 28), saved.getEndDate());
        assertEquals(Set.of("AI", "Robotik"), saved.getKeywords());
        assertEquals(storedFile.fileId(), saved.getSourceFileId());
        assertEquals(storedFile.originalFileName(), saved.getOriginalFileName());
        assertEquals(storedFile.storageKey(), saved.getStorageKey());
        assertEquals(storedFile.contentType(), saved.getContentType());
        assertEquals(storedFile.fileSize(), saved.getFileSize());
        assertEquals(ProjectStatus.AWAITING_REVIEWER, response.status());
    }

    @Test
    void updatesProjectAndRecalculatesEndDate() {
        Project project = project(11L);
        when(projectRepository.findDetailById(11L)).thenReturn(Optional.of(project));
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company(3L)));
        when(assignmentRepository.existsByProjectIdAndStatus(11L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(projectRepository.saveAndFlush(project)).thenReturn(project);

        ProjectResponse response = projectService.updateProject(11L,
                updateRequest(LocalDate.of(2026, 4, 15), 4, Set.of("yeni")),
                "user@example.com", false);

        assertEquals("Guncel Proje", response.name());
        assertEquals(LocalDate.of(2026, 8, 15), response.endDate());
    }

    @Test
    void preventsUserFromUpdatingProjectWithActiveAssignment() {
        when(projectRepository.findDetailById(11L)).thenReturn(Optional.of(project(11L)));
        when(assignmentRepository.existsByProjectIdAndStatus(11L, AssignmentStatus.ACTIVE)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> projectService.updateProject(11L,
                updateRequest(LocalDate.now(), 2, Set.of()), "user@example.com", false));
        verify(projectRepository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    void allowsAdminToUpdateProjectWithActiveAssignment() {
        Project project = project(11L);
        when(projectRepository.findDetailById(11L)).thenReturn(Optional.of(project));
        when(companyRepository.findById(3L)).thenReturn(Optional.of(company(3L)));
        when(projectRepository.saveAndFlush(project)).thenReturn(project);

        ProjectResponse response = projectService.updateProject(11L,
                updateRequest(LocalDate.of(2026, 5, 1), 2, Set.of()), "admin@example.com", true);

        assertEquals(LocalDate.of(2026, 7, 1), response.endDate());
        verify(assignmentRepository, never()).existsByProjectIdAndStatus(11L, AssignmentStatus.ACTIVE);
    }

    @Test
    void rejectsOperationsWhenProjectDoesNotExist() {
        when(projectRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> projectService.getProjectById(99L));
    }

    @Test
    void assignsFirstReviewerAndActivatesAwaitingProject() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.AWAITING_REVIEWER);
        Academician academician = academician(21L, "Ayse", "Demir", "Prof. Dr.");
        User assignedBy = user(7L, "Ali", "Yilmaz");
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findById(21L)).thenReturn(Optional.of(academician));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(assignedBy));
        when(assignmentRepository.saveAndFlush(any(ProjectReviewerAssignment.class)))
                .thenAnswer(invocation -> {
                    ProjectReviewerAssignment assignment = invocation.getArgument(0);
                    assignment.setId(31L);
                    assignment.setCreationTimestamps();
                    return assignment;
                });

        ProjectReviewerAssignmentResponse response = projectService.assignReviewer(
                11L, new ReviewerAssignmentRequest(21L), "user@example.com");

        assertEquals(31L, response.assignmentId());
        assertEquals(11L, response.projectId());
        assertEquals(21L, response.academicianId());
        assertEquals("Ayse Demir", response.academicianName());
        assertEquals("Prof. Dr.", response.academicianTitle());
        assertEquals(AssignmentStatus.ACTIVE, response.status());
        assertNotNull(response.assignedAt());
        assertEquals("Ali Yilmaz", response.assignedByName());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void keepsActiveProjectActiveWhenAssigningSecondReviewer() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findById(21L))
                .thenReturn(Optional.of(academician(21L, "Ayse", "Demir", null)));
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(user(7L, "Admin", "Kullanici")));
        when(assignmentRepository.countByProjectIdAndStatus(11L, AssignmentStatus.ACTIVE))
                .thenReturn(1L);
        when(assignmentRepository.saveAndFlush(any(ProjectReviewerAssignment.class)))
                .thenAnswer(invocation -> {
                    ProjectReviewerAssignment assignment = invocation.getArgument(0);
                    assignment.setCreationTimestamps();
                    return assignment;
                });

        projectService.assignReviewer(
                11L, new ReviewerAssignmentRequest(21L), "admin@example.com");

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void rejectsReviewerAssignmentWhenProjectStatusIsNotEligible() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.COMPLETED);
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findById(21L))
                .thenReturn(Optional.of(academician(21L, "Ayse", "Demir", null)));
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user(7L, "Ali", "Yilmaz")));

        assertThrows(InvalidRequestException.class, () -> projectService.assignReviewer(
                11L, new ReviewerAssignmentRequest(21L), "user@example.com"));
        verify(assignmentRepository, never()).saveAndFlush(any(ProjectReviewerAssignment.class));
    }

    @Test
    void rejectsThirdActiveReviewerWhileHoldingProjectLock() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findById(21L))
                .thenReturn(Optional.of(academician(21L, "Ayse", "Demir", null)));
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(user(7L, "Ali", "Yilmaz")));
        when(assignmentRepository.countByProjectIdAndStatus(11L, AssignmentStatus.ACTIVE))
                .thenReturn(2L);

        assertThrows(InvalidRequestException.class, () -> projectService.assignReviewer(
                11L, new ReviewerAssignmentRequest(21L), "user@example.com"));
        verify(projectRepository).findByIdForReviewerAssignment(11L);
        verify(assignmentRepository, never()).saveAndFlush(any(ProjectReviewerAssignment.class));
    }

    @Test
    void rejectsDuplicateActiveReviewerAndMapsDatabaseRaceToConflict() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        Academician academician = academician(21L, "Ayse", "Demir", null);
        User user = user(7L, "Ali", "Yilmaz");
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(academicianRepository.findById(21L)).thenReturn(Optional.of(academician));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(assignmentRepository.saveAndFlush(any(ProjectReviewerAssignment.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThrows(DuplicateResourceException.class, () -> projectService.assignReviewer(
                11L, new ReviewerAssignmentRequest(21L), "user@example.com"));
    }

    @Test
    void listsTwoActiveReviewerAssignmentsInRepositoryOrderWithSafeFields() {
        Instant firstTime = Instant.parse("2026-08-14T09:00:00Z");
        Instant secondTime = Instant.parse("2026-08-14T10:00:00Z");
        ProjectReviewerAssignment first = assignment(
                31L, 11L, 21L, "Ayse", "Demir", "Prof. Dr.",
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, firstTime);
        ProjectReviewerAssignment second = assignment(
                32L, 11L, 22L, "Mehmet", "Kaya", "Doc. Dr.",
                8L, "Admin", "Kullanici", AssignmentStatus.ACTIVE, secondTime);
        when(projectRepository.existsById(11L)).thenReturn(true);
        when(assignmentRepository.findByProjectIdAndStatusWithPeople(
                11L, AssignmentStatus.ACTIVE)).thenReturn(List.of(first, second));

        List<ProjectReviewerAssignmentResponse> response =
                projectService.listActiveReviewerAssignments(11L);

        assertEquals(List.of(31L, 32L), response.stream()
                .map(ProjectReviewerAssignmentResponse::assignmentId).toList());
        assertEquals("Ayse Demir", response.get(0).academicianName());
        assertEquals("Prof. Dr.", response.get(0).academicianTitle());
        assertEquals("Ali Yilmaz", response.get(0).assignedByName());
        assertEquals(firstTime, response.get(0).assignedAt());
        assertEquals(AssignmentStatus.ACTIVE, response.get(0).status());
        verify(assignmentRepository).findByProjectIdAndStatusWithPeople(
                11L, AssignmentStatus.ACTIVE);
    }

    @Test
    void excludesCompletedAndRemovedAssignmentsByQueryingOnlyActiveStatus() {
        when(projectRepository.existsById(11L)).thenReturn(true);
        Instant assignedAt = Instant.parse("2026-08-14T09:00:00Z");
        ProjectReviewerAssignment active = assignment(
                31L, 11L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, assignedAt);
        ProjectReviewerAssignment completed = assignment(
                30L, 11L, 22L, "Mehmet", "Kaya", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.COMPLETED, assignedAt);
        ProjectReviewerAssignment removed = assignment(
                29L, 11L, 23L, "Fatma", "Celik", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.REMOVED, assignedAt);
        when(assignmentRepository.findByProjectIdAndStatusWithPeople(
                11L, AssignmentStatus.ACTIVE))
                .thenReturn(List.of(active, completed, removed));

        List<ProjectReviewerAssignmentResponse> response =
                projectService.listActiveReviewerAssignments(11L);

        assertEquals(List.of(31L), response.stream()
                .map(ProjectReviewerAssignmentResponse::assignmentId).toList());
        verify(assignmentRepository, never()).findByProjectIdAndStatusWithPeople(
                eq(11L), eq(AssignmentStatus.COMPLETED));
        verify(assignmentRepository, never()).findByProjectIdAndStatusWithPeople(
                eq(11L), eq(AssignmentStatus.REMOVED));
    }

    @Test
    void returnsEmptyListWhenProjectHasNoActiveAssignments() {
        when(projectRepository.existsById(11L)).thenReturn(true);
        when(assignmentRepository.findByProjectIdAndStatusWithPeople(
                11L, AssignmentStatus.ACTIVE)).thenReturn(List.of());

        assertEquals(List.of(), projectService.listActiveReviewerAssignments(11L));
    }

    @Test
    void rejectsActiveAssignmentListWhenProjectDoesNotExist() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> projectService.listActiveReviewerAssignments(99L));
        verify(assignmentRepository, never()).findByProjectIdAndStatusWithPeople(
                any(), any());
    }

    @Test
    void listsRemovedAndCompletedHistoryNewestFirstAndKeepsRepeatedAcademicianPeriods() {
        Instant newestEnd = Instant.parse("2026-08-14T12:00:00Z");
        Instant tiedEnd = Instant.parse("2026-08-14T11:00:00Z");
        ProjectReviewerAssignment newest = historicalAssignment(
                50L, 11L, 21L, AssignmentStatus.REMOVED, newestEnd);
        ProjectReviewerAssignment tiedHigherId = historicalAssignment(
                49L, 11L, 22L, AssignmentStatus.COMPLETED, tiedEnd);
        ProjectReviewerAssignment repeatedAcademician = historicalAssignment(
                48L, 11L, 21L, AssignmentStatus.REMOVED, tiedEnd);
        ProjectReviewerAssignment active = historicalAssignment(
                51L, 11L, 23L, AssignmentStatus.ACTIVE, null);
        when(projectRepository.existsById(11L)).thenReturn(true);
        when(assignmentRepository.findHistoryByProjectIdWithPeople(
                eq(11L), eq(Set.of(
                        AssignmentStatus.REMOVED,
                        AssignmentStatus.COMPLETED,
                        AssignmentStatus.CANCELLED
                )))).thenReturn(List.of(
                newest, tiedHigherId, repeatedAcademician, active));

        List<ProjectReviewerAssignmentHistoryResponse> response =
                projectService.listReviewerAssignmentHistory(11L);

        assertEquals(List.of(50L, 49L, 48L), response.stream()
                .map(ProjectReviewerAssignmentHistoryResponse::assignmentId).toList());
        assertEquals(List.of(
                AssignmentStatus.REMOVED,
                AssignmentStatus.COMPLETED,
                AssignmentStatus.REMOVED
        ), response.stream()
                .map(ProjectReviewerAssignmentHistoryResponse::status).toList());
        assertEquals(2L, response.stream()
                .filter(item -> item.academicianId().equals(21L))
                .count());
        assertEquals(newestEnd, response.get(0).endedAt());
        assertEquals(tiedEnd, response.get(1).endedAt());
        verify(assignmentRepository).findHistoryByProjectIdWithPeople(
                11L, Set.of(
                        AssignmentStatus.REMOVED,
                        AssignmentStatus.COMPLETED,
                        AssignmentStatus.CANCELLED
                ));
        verify(assignmentRepository, never()).saveAndFlush(
                any(ProjectReviewerAssignment.class));
        verify(assignmentRepository, never()).delete(any(ProjectReviewerAssignment.class));
        verify(projectRepository, never()).saveAndFlush(any(Project.class));
    }

    @Test
    void returnsEmptyReviewerAssignmentHistory() {
        when(projectRepository.existsById(11L)).thenReturn(true);
        when(assignmentRepository.findHistoryByProjectIdWithPeople(
                eq(11L), eq(Set.of(
                        AssignmentStatus.REMOVED,
                        AssignmentStatus.COMPLETED,
                        AssignmentStatus.CANCELLED
                )))).thenReturn(List.of());

        assertEquals(List.of(), projectService.listReviewerAssignmentHistory(11L));
    }

    @Test
    void rejectsReviewerAssignmentHistoryWhenProjectDoesNotExist() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> projectService.listReviewerAssignmentHistory(99L));
        verify(assignmentRepository, never()).findHistoryByProjectIdWithPeople(
                anyLong(), anyCollection());
    }

    @Test
    void removesOneOfTwoActiveReviewersAndKeepsProjectActiveWithoutDeletingHistory() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        Instant assignedAt = Instant.parse("2026-08-14T09:00:00Z");
        ProjectReviewerAssignment assignment = assignment(
                31L, 11L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, assignedAt);
        User assignedBy = assignment.getAssignedBy();
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.saveAndFlush(assignment)).thenReturn(assignment);
        when(assignmentRepository.countByProjectIdAndStatus(
                11L, AssignmentStatus.ACTIVE)).thenReturn(1L);

        Instant beforeRemoval = Instant.now();
        projectService.removeReviewerAssignment(11L, 31L);
        Instant afterRemoval = Instant.now();

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertEquals(AssignmentStatus.REMOVED, assignment.getStatus());
        assertNotNull(assignment.getEndedAt());
        assertFalse(assignment.getEndedAt().isBefore(beforeRemoval));
        assertFalse(assignment.getEndedAt().isAfter(afterRemoval));
        assertEquals(assignedBy, assignment.getAssignedBy());
        assertEquals(assignedAt, assignment.getAssignedAt());
        verify(projectRepository).findByIdForReviewerAssignment(11L);
        verify(assignmentRepository).saveAndFlush(assignment);
        verify(assignmentRepository, never()).delete(any(ProjectReviewerAssignment.class));
        verify(assignmentRepository, never()).deleteById(anyLong());
    }

    @Test
    void removesOnlyActiveReviewerAndMovesProjectBackToAwaitingReviewer() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        ProjectReviewerAssignment assignment = assignment(
                31L, 11L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, Instant.now());
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(assignment));
        when(assignmentRepository.saveAndFlush(assignment)).thenReturn(assignment);
        when(assignmentRepository.countByProjectIdAndStatus(
                11L, AssignmentStatus.ACTIVE)).thenReturn(0L);

        projectService.removeReviewerAssignment(11L, 31L);

        assertEquals(ProjectStatus.AWAITING_REVIEWER, project.getStatus());
        assertEquals(AssignmentStatus.REMOVED, assignment.getStatus());
    }

    @Test
    void rejectsRemovingAssignmentThatBelongsToAnotherProject() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        ProjectReviewerAssignment assignment = assignment(
                31L, 12L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, Instant.now());
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(assignment));

        assertThrows(InvalidRequestException.class,
                () -> projectService.removeReviewerAssignment(11L, 31L));
        verify(assignmentRepository, never()).saveAndFlush(
                any(ProjectReviewerAssignment.class));
    }

    @Test
    void rejectsRemovingAssignmentThatIsAlreadyRemovedOrCompleted() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.ACTIVE);
        ProjectReviewerAssignment removed = assignment(
                31L, 11L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.REMOVED, Instant.now());
        ProjectReviewerAssignment completed = assignment(
                32L, 11L, 22L, "Mehmet", "Kaya", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.COMPLETED, Instant.now());
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(removed));
        when(assignmentRepository.findById(32L)).thenReturn(Optional.of(completed));

        assertThrows(DuplicateResourceException.class,
                () -> projectService.removeReviewerAssignment(11L, 31L));
        assertThrows(DuplicateResourceException.class,
                () -> projectService.removeReviewerAssignment(11L, 32L));
        verify(assignmentRepository, never()).saveAndFlush(
                any(ProjectReviewerAssignment.class));
    }

    @Test
    void rejectsRemovingReviewerFromCompletedOrCancelledProject() {
        Project completedProject = project(11L);
        completedProject.setStatus(ProjectStatus.COMPLETED);
        Project cancelledProject = project(11L);
        cancelledProject.setStatus(ProjectStatus.CANCELLED);
        ProjectReviewerAssignment firstAssignment = assignment(
                31L, 11L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, Instant.now());
        ProjectReviewerAssignment secondAssignment = assignment(
                32L, 11L, 22L, "Mehmet", "Kaya", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, Instant.now());
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(completedProject), Optional.of(cancelledProject));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(firstAssignment));
        when(assignmentRepository.findById(32L)).thenReturn(Optional.of(secondAssignment));

        assertThrows(InvalidRequestException.class,
                () -> projectService.removeReviewerAssignment(11L, 31L));
        assertThrows(InvalidRequestException.class,
                () -> projectService.removeReviewerAssignment(11L, 32L));
        verify(assignmentRepository, never()).saveAndFlush(
                any(ProjectReviewerAssignment.class));
    }

    @Test
    void rejectsActiveAssignmentRemovalWhenProjectIsAwaitingReviewer() {
        Project project = project(11L);
        project.setStatus(ProjectStatus.AWAITING_REVIEWER);
        ProjectReviewerAssignment assignment = assignment(
                31L, 11L, 21L, "Ayse", "Demir", null,
                7L, "Ali", "Yilmaz", AssignmentStatus.ACTIVE, Instant.now());
        when(projectRepository.findByIdForReviewerAssignment(11L))
                .thenReturn(Optional.of(project));
        when(assignmentRepository.findById(31L)).thenReturn(Optional.of(assignment));

        assertThrows(InvalidRequestException.class,
                () -> projectService.removeReviewerAssignment(11L, 31L));
        verify(assignmentRepository, never()).saveAndFlush(
                any(ProjectReviewerAssignment.class));
    }

    private CreateProjectRequest createRequest(LocalDate startDate, int duration, Set<String> keywords) {
        return new CreateProjectRequest(3L, " Proje ", " Yonetici ", startDate, duration,
                "Kategori", "Sektor", null, null, null, null, null, keywords);
    }

    private UpdateProjectRequest updateRequest(LocalDate startDate, int duration, Set<String> keywords) {
        return new UpdateProjectRequest(3L, " Guncel Proje ", " Yonetici ", startDate, duration,
                "Kategori", "Sektor", null, null, null, null, null, keywords);
    }

    private Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        company.setName("Teknokent Sirket");
        company.setCompanyType(CompanyType.TGB);
        return company;
    }

    private User user(Long id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private Academician academician(
            Long id,
            String firstName,
            String lastName,
            String title
    ) {
        Academician academician = new Academician();
        academician.setId(id);
        academician.setFirstName(firstName);
        academician.setLastName(lastName);
        academician.setTitle(title);
        return academician;
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setCompany(company(1L));
        project.setCreatedBy(user(2L, "Eski", "Kullanici"));
        project.setName("Eski Proje");
        project.setStatus(ProjectStatus.DRAFT);
        return project;
    }

    private ProjectReviewerAssignment assignment(
            Long assignmentId,
            Long projectId,
            Long academicianId,
            String academicianFirstName,
            String academicianLastName,
            String academicianTitle,
            Long assignedById,
            String assignedByFirstName,
            String assignedByLastName,
            AssignmentStatus status,
            Instant assignedAt
    ) {
        ProjectReviewerAssignment assignment = new ProjectReviewerAssignment();
        assignment.setId(assignmentId);
        assignment.setProject(project(projectId));
        assignment.setAcademician(academician(
                academicianId, academicianFirstName, academicianLastName, academicianTitle));
        assignment.setAssignedBy(user(
                assignedById, assignedByFirstName, assignedByLastName));
        assignment.setStatus(status);
        assignment.setAssignedAt(assignedAt);
        return assignment;
    }

    private ProjectReviewerAssignment historicalAssignment(
            Long assignmentId,
            Long projectId,
            Long academicianId,
            AssignmentStatus status,
            Instant endedAt
    ) {
        ProjectReviewerAssignment assignment = assignment(
                assignmentId,
                projectId,
                academicianId,
                "Akademisyen",
                academicianId.toString(),
                "Prof. Dr.",
                7L,
                "Ali",
                "Yilmaz",
                status,
                Instant.parse("2026-08-01T09:00:00Z")
        );
        assignment.setEndedAt(endedAt);
        return assignment;
    }

    private Project savedProject() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
