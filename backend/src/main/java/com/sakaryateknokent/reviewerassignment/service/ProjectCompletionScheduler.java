package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

@Service
@Slf4j
public class ProjectCompletionScheduler {

    private static final Set<ProjectStatus> COMPLETABLE_STATUSES = Set.of(
            ProjectStatus.AWAITING_REVIEWER,
            ProjectStatus.ACTIVE
    );

    private final ProjectRepository projectRepository;
    private final ProjectCompletionService completionService;
    private final Clock clock;
    private final ZoneId businessZone;

    public ProjectCompletionScheduler(
            ProjectRepository projectRepository,
            ProjectCompletionService completionService,
            Clock clock,
            @Value("${project.completion.zone:Europe/Istanbul}") String businessZone
    ) {
        this.projectRepository = projectRepository;
        this.completionService = completionService;
        this.clock = clock;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Scheduled(
            cron = "${project.completion.cron:0 * * * * *}",
            zone = "${project.completion.zone:Europe/Istanbul}"
    )
    public void completeDueProjects() {
        LocalDate businessDate = LocalDate.now(clock.withZone(businessZone));
        for (Long projectId : projectRepository.findCompletionCandidateIds(
                COMPLETABLE_STATUSES, businessDate)) {
            completeOne(projectId, businessDate);
        }
    }

    private void completeOne(Long projectId, LocalDate businessDate) {
        try {
            completionService.completeIfDue(projectId, businessDate, businessZone);
        } catch (RuntimeException exception) {
            log.warn(
                    "Automatic project completion deferred for projectId={}",
                    projectId,
                    exception
            );
        }
    }
}
