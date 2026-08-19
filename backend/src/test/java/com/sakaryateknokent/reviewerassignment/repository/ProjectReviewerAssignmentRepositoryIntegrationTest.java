package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.ProjectReviewerAssignment;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
class ProjectReviewerAssignmentRepositoryIntegrationTest {

    private final ProjectReviewerAssignmentRepository assignmentRepository;
    private final EntityManager entityManager;

    @Autowired
    ProjectReviewerAssignmentRepositoryIntegrationTest(
            ProjectReviewerAssignmentRepository assignmentRepository,
            EntityManager entityManager
    ) {
        this.assignmentRepository = assignmentRepository;
        this.entityManager = entityManager;
    }

    @Test
    void aggregatesDistinctActiveAndCompletedProjectsAndExcludesRemoved() {
        User user = persistUser();
        Company company = persistCompany();
        Academician academician = persistAcademician();
        Project activeAndCompleted = persistProject(company, user, "P1");
        Project completed = persistProject(company, user, "P2");
        Project removed = persistProject(company, user, "P3");
        Project active = persistProject(company, user, "P4");

        persistAssignment(activeAndCompleted, academician, user,
                AssignmentStatus.ACTIVE);
        persistAssignment(activeAndCompleted, academician, user,
                AssignmentStatus.COMPLETED);
        persistAssignment(activeAndCompleted, academician, user,
                AssignmentStatus.COMPLETED);
        persistAssignment(completed, academician, user,
                AssignmentStatus.COMPLETED);
        persistAssignment(removed, academician, user,
                AssignmentStatus.REMOVED);
        persistAssignment(active, academician, user,
                AssignmentStatus.ACTIVE);
        entityManager.flush();

        AcademicianProjectSummary summary = assignmentRepository
                .findProjectSummary(
                        academician.getId(),
                        AssignmentStatus.ACTIVE,
                        AssignmentStatus.COMPLETED
                )
                .orElseThrow();

        assertEquals(2L, summary.getActiveProjectCount());
        assertEquals(2L, summary.getCompletedProjectCount());
        assertEquals(3L, summary.getTotalProjectCount());

        List<AcademicianProjectWorkload> workloads = assignmentRepository
                .findWorkloads(
                        List.of(academician.getId()),
                        removed.getId(),
                        AssignmentStatus.ACTIVE,
                        AssignmentStatus.COMPLETED
                );

        assertEquals(1, workloads.size());
        assertEquals(2L, workloads.getFirst().getActiveProjectCount());
        assertEquals(3L, workloads.getFirst().getTotalProjectCount());
        assertEquals(0L, workloads.getFirst()
                .getCurrentProjectActiveAssignmentCount());
    }

    @Test
    void summaryHasNoAggregationRowWhenAcademicianHasOnlyRemovedAssignment() {
        User user = persistUser();
        Company company = persistCompany();
        Academician academician = persistAcademician();
        Project project = persistProject(company, user, "Removed only");
        persistAssignment(project, academician, user, AssignmentStatus.REMOVED);
        entityManager.flush();

        assertTrue(assignmentRepository.findProjectSummary(
                academician.getId(),
                AssignmentStatus.ACTIVE,
                AssignmentStatus.COMPLETED
        ).isEmpty());
    }

    private User persistUser() {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("summary-" + System.nanoTime() + "@example.com");
        user.setPhone("5550000000");
        user.setPasswordHash("hash");
        entityManager.persist(user);
        return user;
    }

    private Company persistCompany() {
        Company company = new Company();
        company.setName("Summary Test Company " + System.nanoTime());
        company.setCompanyType(CompanyType.TGB);
        entityManager.persist(company);
        return company;
    }

    private Academician persistAcademician() {
        Academician academician = new Academician();
        academician.setFirstName("Summary");
        academician.setLastName("Academician " + System.nanoTime());
        entityManager.persist(academician);
        return academician;
    }

    private Project persistProject(
            Company company,
            User user,
            String name
    ) {
        Project project = new Project();
        project.setCompany(company);
        project.setCreatedBy(user);
        project.setName(name + " " + System.nanoTime());
        entityManager.persist(project);
        return project;
    }

    private void persistAssignment(
            Project project,
            Academician academician,
            User user,
            AssignmentStatus status
    ) {
        ProjectReviewerAssignment assignment =
                new ProjectReviewerAssignment();
        assignment.setProject(project);
        assignment.setAcademician(academician);
        assignment.setAssignedBy(user);
        assignment.setStatus(status);
        entityManager.persist(assignment);
    }
}
