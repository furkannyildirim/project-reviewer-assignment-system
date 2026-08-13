package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectListResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectActiveReviewerCount;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ProjectReviewerAssignmentRepository assignmentRepository;

    public ProjectResponse createProject(
            CreateProjectRequest request,
            String currentUserEmail
    ) {
        Company company = findCompany(request.companyId());
        User createdBy = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici bulunamadi"));

        Project project = new Project();
        project.setCompany(company);
        project.setCreatedBy(createdBy);
        project.setStatus(ProjectStatus.DRAFT);
        apply(project, request);

        return toResponse(projectRepository.saveAndFlush(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        return toResponse(findProjectDetail(id));
    }

    @Transactional(readOnly = true)
    public Page<ProjectListResponse> listProjects(Pageable pageable) {
        Page<Project> projectPage = projectRepository.findAll(pageable);
        Map<Long, Long> activeCounts = activeCounts(projectPage.getContent());
        return new PageImpl<>(
                projectPage.getContent().stream()
                        .map(project -> toListResponse(project,
                                activeCounts.getOrDefault(project.getId(), 0L)))
                        .toList(),
                projectPage.getPageable(),
                projectPage.getTotalElements()
        );
    }

    public ProjectResponse updateProject(
            Long id,
            UpdateProjectRequest request,
            String currentUserEmail,
            boolean isAdmin
    ) {
        Project project = findProjectDetail(id);
        if (!isAdmin && assignmentRepository.existsByProjectIdAndStatus(
                project.getId(), AssignmentStatus.ACTIVE)) {
            throw new IllegalStateException(
                    "Aktif hakem atamasi olan proje sadece yonetici tarafindan guncellenebilir"
            );
        }

        project.setCompany(findCompany(request.companyId()));
        apply(project, request);
        return toResponse(projectRepository.saveAndFlush(project));
    }

    private Project findProjectDetail(Long id) {
        return projectRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Sirket bulunamadi"));
    }

    private void apply(Project project, CreateProjectRequest request) {
        project.setName(request.name().trim());
        project.setProjectManager(normalizeOptionalText(request.projectManager()));
        project.setStartDate(request.startDate());
        project.setDurationMonths(request.durationMonths());
        project.setEndDate(request.startDate().plusMonths(request.durationMonths()));
        project.setCategory(normalizeOptionalText(request.category()));
        project.setSector(normalizeOptionalText(request.sector()));
        project.setSummary(normalizeOptionalText(request.summary()));
        project.setResearchAndDevelopmentNature(normalizeOptionalText(request.researchAndDevelopmentNature()));
        project.setInnovativeAspect(normalizeOptionalText(request.innovativeAspect()));
        project.setMainTechnologyArea(normalizeOptionalText(request.mainTechnologyArea()));
        project.setProjectDetail(normalizeOptionalText(request.projectDetail()));
        replaceKeywords(project, request.keywords());
    }

    private void apply(Project project, UpdateProjectRequest request) {
        project.setName(request.name().trim());
        project.setProjectManager(normalizeOptionalText(request.projectManager()));
        project.setStartDate(request.startDate());
        project.setDurationMonths(request.durationMonths());
        project.setEndDate(request.startDate().plusMonths(request.durationMonths()));
        project.setCategory(normalizeOptionalText(request.category()));
        project.setSector(normalizeOptionalText(request.sector()));
        project.setSummary(normalizeOptionalText(request.summary()));
        project.setResearchAndDevelopmentNature(normalizeOptionalText(request.researchAndDevelopmentNature()));
        project.setInnovativeAspect(normalizeOptionalText(request.innovativeAspect()));
        project.setMainTechnologyArea(normalizeOptionalText(request.mainTechnologyArea()));
        project.setProjectDetail(normalizeOptionalText(request.projectDetail()));
        replaceKeywords(project, request.keywords());
    }

    private void replaceKeywords(Project project, Set<String> keywords) {
        project.getKeywords().clear();
        project.getKeywords().addAll(normalizeKeywords(keywords));
    }

    private Set<String> normalizeKeywords(Set<String> keywords) {
        if (keywords == null) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        Set<String> canonical = new LinkedHashSet<>();
        for (String keyword : keywords) {
            String trimmed = normalizeOptionalText(keyword);
            if (trimmed != null && canonical.add(trimmed.toLowerCase(Locale.ROOT))) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private Map<Long, Long> activeCounts(Collection<Project> projects) {
        if (projects.isEmpty()) {
            return Map.of();
        }
        return assignmentRepository.countActiveByProjectIds(
                        projects.stream().map(Project::getId).toList(),
                        AssignmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(ProjectActiveReviewerCount::getProjectId,
                        ProjectActiveReviewerCount::getActiveReviewerCount));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProjectResponse toResponse(Project project) {
        User user = project.getCreatedBy();
        return new ProjectResponse(project.getId(), project.getCompany().getId(),
                project.getCompany().getName(), project.getCompany().getCompanyType(),
                user.getId(), user.getFirstName() + " " + user.getLastName(),
                project.getName(), project.getProjectManager(), project.getStartDate(),
                project.getDurationMonths(), project.getEndDate(), project.getCategory(),
                project.getSector(), project.getSummary(), project.getResearchAndDevelopmentNature(),
                project.getInnovativeAspect(), project.getMainTechnologyArea(),
                project.getProjectDetail(), Set.copyOf(project.getKeywords()), project.getStatus(),
                project.getSourceFileId(), project.getOriginalFileName(), project.getContentType(),
                project.getFileSize(), project.getCreatedAt(), project.getUpdatedAt());
    }

    private ProjectListResponse toListResponse(Project project, long activeReviewerCount) {
        return new ProjectListResponse(project.getId(), project.getName(),
                project.getCompany().getId(), project.getCompany().getName(),
                project.getCompany().getCompanyType(), project.getCategory(), project.getSector(),
                project.getStartDate(), project.getEndDate(), project.getStatus(),
                activeReviewerCount, project.getCreatedAt());
    }
}
