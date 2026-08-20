package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.dto.CreateProjectFromUploadRequest;
import com.sakaryateknokent.reviewerassignment.dto.ProjectListResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentHistoryResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectReviewerAssignmentResponse;
import com.sakaryateknokent.reviewerassignment.dto.ProjectResponse;
import com.sakaryateknokent.reviewerassignment.dto.ReviewerAssignmentRequest;
import com.sakaryateknokent.reviewerassignment.dto.UpdateProjectRequest;
import com.sakaryateknokent.reviewerassignment.entity.Academician;
import com.sakaryateknokent.reviewerassignment.entity.Company;
import com.sakaryateknokent.reviewerassignment.entity.Project;
import com.sakaryateknokent.reviewerassignment.entity.ProjectReviewerAssignment;
import com.sakaryateknokent.reviewerassignment.entity.User;
import com.sakaryateknokent.reviewerassignment.enums.AssignmentStatus;
import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.exception.DuplicateResourceException;
import com.sakaryateknokent.reviewerassignment.exception.InvalidRequestException;
import com.sakaryateknokent.reviewerassignment.exception.ResourceNotFoundException;
import com.sakaryateknokent.reviewerassignment.repository.AcademicianRepository;
import com.sakaryateknokent.reviewerassignment.repository.CompanyRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectActiveReviewerCount;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import com.sakaryateknokent.reviewerassignment.repository.ProjectReviewerAssignmentRepository;
import com.sakaryateknokent.reviewerassignment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProjectService {

    private static final Set<AssignmentStatus> ASSIGNMENT_HISTORY_STATUSES = Set.of(
            AssignmentStatus.REMOVED,
            AssignmentStatus.COMPLETED
    );

    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final ProjectReviewerAssignmentRepository assignmentRepository;
    private final AcademicianRepository academicianRepository;
    private final ProjectFileStorageService fileStorageService;

    ProjectResponse createProjectFromUpload(
            CreateProjectFromUploadRequest request,
            Company company,
            User createdBy,
            StoredProjectFile storedFile
    ) {
        Project project = new Project();
        project.setCompany(company);
        project.setCreatedBy(createdBy);
        project.setStatus(ProjectStatus.AWAITING_REVIEWER);
        apply(project, request);
        project.setSourceFileId(storedFile.fileId());
        project.setOriginalFileName(storedFile.originalFileName());
        project.setStorageKey(storedFile.storageKey());
        project.setContentType(storedFile.contentType());
        project.setFileSize(storedFile.fileSize());
        return toResponse(projectRepository.saveAndFlush(project));
    }

    Company findCompanyForProject(Long companyId) {
        return findCompany(companyId);
    }

    User findUserForProject(String currentUserEmail) {
        return userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici bulunamadi"));
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

    public ProjectReviewerAssignmentResponse assignReviewer(
            Long projectId,
            ReviewerAssignmentRequest request,
            String currentUserEmail
    ) {
        Project project = projectRepository.findByIdForReviewerAssignment(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));
        Academician academician = academicianRepository.findById(request.academicianId())
                .orElseThrow(() -> new ResourceNotFoundException("Akademisyen bulunamadi"));
        User assignedBy = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanici bulunamadi"));

        if (project.getStatus() != ProjectStatus.AWAITING_REVIEWER
                && project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidRequestException(
                    "Yalnizca hakem bekleyen veya aktif projelere hakem atanabilir"
            );
        }
        if (assignmentRepository.existsByProjectIdAndAcademicianIdAndStatus(
                projectId, academician.getId(), AssignmentStatus.ACTIVE)) {
            throw new DuplicateResourceException(
                    "Akademisyen bu projeye zaten aktif hakem olarak atanmis"
            );
        }
        if (assignmentRepository.countByProjectIdAndStatus(
                projectId, AssignmentStatus.ACTIVE) >= 2) {
            throw new InvalidRequestException("Bir projeye en fazla iki aktif hakem atanabilir");
        }

        ProjectReviewerAssignment assignment = new ProjectReviewerAssignment();
        assignment.setProject(project);
        assignment.setAcademician(academician);
        assignment.setAssignedBy(assignedBy);
        assignment.setStatus(AssignmentStatus.ACTIVE);

        if (project.getStatus() == ProjectStatus.AWAITING_REVIEWER) {
            project.setStatus(ProjectStatus.ACTIVE);
        }

        try {
            ProjectReviewerAssignment saved = assignmentRepository.saveAndFlush(assignment);
            return toAssignmentResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateResourceException(
                    "Akademisyen bu projeye zaten aktif hakem olarak atanmis"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectReviewerAssignmentResponse> listActiveReviewerAssignments(
            Long projectId
    ) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Proje bulunamadi");
        }
        return assignmentRepository.findByProjectIdAndStatusWithPeople(
                        projectId, AssignmentStatus.ACTIVE)
                .stream()
                .filter(assignment -> assignment.getStatus() == AssignmentStatus.ACTIVE)
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectReviewerAssignmentHistoryResponse> listReviewerAssignmentHistory(
            Long projectId
    ) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Proje bulunamadi");
        }
        return assignmentRepository.findHistoryByProjectIdWithPeople(
                        projectId, ASSIGNMENT_HISTORY_STATUSES)
                .stream()
                .filter(assignment -> ASSIGNMENT_HISTORY_STATUSES.contains(
                        assignment.getStatus()))
                .map(this::toAssignmentHistoryResponse)
                .toList();
    }

    public void removeReviewerAssignment(Long projectId, Long assignmentId) {
        Project project = projectRepository.findByIdForReviewerAssignment(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));
        ProjectReviewerAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Hakem atamasi bulunamadi"));

        if (!projectId.equals(assignment.getProject().getId())) {
            throw new InvalidRequestException("Hakem atamasi belirtilen projeye ait degil");
        }
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new InvalidRequestException(
                    "Yalnizca aktif projelerdeki hakem atamalari kaldirilabilir"
            );
        }
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new DuplicateResourceException("Hakem atamasi artik aktif degil");
        }

        assignment.setStatus(AssignmentStatus.REMOVED);
        assignment.setEndedAt(Instant.now());
        assignmentRepository.saveAndFlush(assignment);

        if (assignmentRepository.countByProjectIdAndStatus(
                projectId, AssignmentStatus.ACTIVE) == 0) {
            project.setStatus(ProjectStatus.AWAITING_REVIEWER);
        }
    }

    public void deleteProject(Long projectId) {
        Project project = projectRepository.findByIdForReviewerAssignment(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));

        if (project.getStatus() == ProjectStatus.COMPLETED) {
            throw new DuplicateResourceException(
                    "Tamamlanmis projeler kalici olarak silinemez"
            );
        }

        String permanentStorageKey = project.getStorageKey();
        assignmentRepository.deleteAllByProjectId(projectId);
        projectRepository.delete(project);

        if (permanentStorageKey != null) {
            registerProjectFileDeletionAfterCommit(
                    projectId,
                    permanentStorageKey
            );
        }
    }

    private void registerProjectFileDeletionAfterCommit(
            Long projectId,
            String permanentStorageKey
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException(
                    "Proje dosyasi temizligi icin transaction gerekli"
            );
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            return;
                        }
                        try {
                            fileStorageService.deleteProjectFile(
                                    permanentStorageKey
                            );
                        } catch (RuntimeException exception) {
                            log.warn(
                                    "Committed project file cleanup failed for projectId={}",
                                    projectId
                            );
                        }
                    }
                }
        );
    }

    private Project findProjectDetail(Long id) {
        return projectRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proje bulunamadi"));
    }

    private Company findCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Sirket bulunamadi"));
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

    private void apply(Project project, CreateProjectFromUploadRequest request) {
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
        keywords.stream()
                .map(this::normalizeOptionalText)
                .filter(java.util.Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER
                        .thenComparing(Comparator.naturalOrder()))
                .forEach(trimmed -> {
                    if (canonical.add(trimmed.toLowerCase(Locale.ROOT))) {
                        normalized.add(trimmed);
                    }
                });
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

    private ProjectReviewerAssignmentResponse toAssignmentResponse(
            ProjectReviewerAssignment assignment
    ) {
        Academician academician = assignment.getAcademician();
        User assignedBy = assignment.getAssignedBy();
        return new ProjectReviewerAssignmentResponse(
                assignment.getId(),
                assignment.getProject().getId(),
                academician.getId(),
                academician.getFirstName() + " " + academician.getLastName(),
                academician.getTitle(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignedBy.getFirstName() + " " + assignedBy.getLastName()
        );
    }

    private ProjectReviewerAssignmentHistoryResponse toAssignmentHistoryResponse(
            ProjectReviewerAssignment assignment
    ) {
        Academician academician = assignment.getAcademician();
        User assignedBy = assignment.getAssignedBy();
        return new ProjectReviewerAssignmentHistoryResponse(
                assignment.getId(),
                assignment.getProject().getId(),
                academician.getId(),
                academician.getFirstName() + " " + academician.getLastName(),
                academician.getTitle(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getEndedAt(),
                assignedBy.getFirstName() + " " + assignedBy.getLastName()
        );
    }
}
