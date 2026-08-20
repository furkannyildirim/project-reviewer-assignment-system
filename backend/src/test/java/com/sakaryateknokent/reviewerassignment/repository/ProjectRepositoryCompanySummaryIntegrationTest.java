package com.sakaryateknokent.reviewerassignment.repository;

import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.CompanyType;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "JWT_SECRET", matches = ".+")
class ProjectRepositoryCompanySummaryIntegrationTest {

    private final ProjectRepository projectRepository;
    private final EntityManager entityManager;

    @Autowired
    ProjectRepositoryCompanySummaryIntegrationTest(
            ProjectRepository projectRepository,
            EntityManager entityManager
    ) {
        this.projectRepository = projectRepository;
        this.entityManager = entityManager;
    }

    @Test
    void aggregatesEveryStatusForOnlyTheRequestedCompany() {
        User user = persistUser();
        Company selectedCompany = persistCompany("Selected");
        Company otherCompany = persistCompany("Other");

        persistProject(selectedCompany, user, ProjectStatus.DRAFT);
        persistProject(selectedCompany, user, ProjectStatus.AWAITING_REVIEWER);
        persistProject(selectedCompany, user, ProjectStatus.ACTIVE);
        persistProject(selectedCompany, user, ProjectStatus.COMPLETED);
        persistProject(selectedCompany, user, ProjectStatus.CANCELLED);
        persistProject(selectedCompany, user, ProjectStatus.ACTIVE);
        persistProject(otherCompany, user, ProjectStatus.COMPLETED);
        entityManager.flush();

        CompanyProjectSummary summary = projectRepository
                .findCompanyProjectSummary(
                        selectedCompany.getId(),
                        ProjectStatus.DRAFT,
                        ProjectStatus.AWAITING_REVIEWER,
                        ProjectStatus.ACTIVE,
                        ProjectStatus.COMPLETED,
                        ProjectStatus.CANCELLED
                )
                .orElseThrow();

        assertEquals(selectedCompany.getId(), summary.getCompanyId());
        assertEquals(6L, summary.getTotalProjectCount());
        assertEquals(1L, summary.getDraftProjectCount());
        assertEquals(1L, summary.getAwaitingReviewerProjectCount());
        assertEquals(2L, summary.getActiveProjectCount());
        assertEquals(1L, summary.getCompletedProjectCount());
        assertEquals(1L, summary.getCancelledProjectCount());
    }

    private User persistUser() {
        User user = new User();
        user.setFirstName("Summary");
        user.setLastName("User");
        user.setEmail("company-summary-" + System.nanoTime() + "@example.com");
        user.setPhone("5550000000");
        user.setPasswordHash("hash");
        entityManager.persist(user);
        return user;
    }

    private Company persistCompany(String name) {
        Company company = new Company();
        company.setName(name + " " + System.nanoTime());
        company.setCompanyType(CompanyType.TGB);
        entityManager.persist(company);
        return company;
    }

    private void persistProject(
            Company company,
            User user,
            ProjectStatus status
    ) {
        Project project = new Project();
        project.setCompany(company);
        project.setCreatedBy(user);
        project.setName(status + " " + System.nanoTime());
        project.setStatus(status);
        entityManager.persist(project);
    }
}
