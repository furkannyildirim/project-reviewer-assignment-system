package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    private final ProjectService projectService = new ProjectService(
            projectRepository, companyRepository, userRepository, assignmentRepository);

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

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setCompany(company(1L));
        project.setCreatedBy(user(2L, "Eski", "Kullanici"));
        project.setName("Eski Proje");
        project.setStatus(ProjectStatus.DRAFT);
        return project;
    }

    private Project savedProject() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }
}
