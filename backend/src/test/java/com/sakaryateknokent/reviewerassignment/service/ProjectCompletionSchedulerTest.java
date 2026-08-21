package com.sakaryateknokent.reviewerassignment.service;

import com.sakaryateknokent.reviewerassignment.enums.ProjectStatus;
import com.sakaryateknokent.reviewerassignment.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCompletionSchedulerTest {

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectCompletionService completionService =
            mock(ProjectCompletionService.class);

    @Test
    void runsEveryMinuteInIstanbulByDefault() throws Exception {
        Scheduled scheduled = ProjectCompletionScheduler.class
                .getMethod("completeDueProjects")
                .getAnnotation(Scheduled.class);

        assertEquals(
                "${project.completion.cron:0 * * * * *}",
                scheduled.cron()
        );
        assertEquals(
                "${project.completion.zone:Europe/Istanbul}",
                scheduled.zone()
        );
    }

    @Test
    void usesIstanbulBusinessDateAndProcessesEveryCandidate() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-19T21:00:00Z"),
                ZoneOffset.UTC
        );
        LocalDate expectedDate = LocalDate.of(2026, 8, 20);
        Set<ProjectStatus> expectedStatuses = Set.of(
                ProjectStatus.AWAITING_REVIEWER,
                ProjectStatus.ACTIVE
        );
        when(projectRepository.findCompletionCandidateIds(expectedStatuses, expectedDate))
                .thenReturn(List.of(11L, 12L));
        ProjectCompletionScheduler scheduler = new ProjectCompletionScheduler(
                projectRepository,
                completionService,
                clock,
                "Europe/Istanbul"
        );

        scheduler.completeDueProjects();

        verify(projectRepository).findCompletionCandidateIds(
                expectedStatuses,
                expectedDate
        );
        verify(completionService).completeIfDue(11L, expectedDate, ISTANBUL);
        verify(completionService).completeIfDue(12L, expectedDate, ISTANBUL);
    }

    @Test
    void failureForOneProjectDoesNotPreventFollowingCandidate() {
        LocalDate expectedDate = LocalDate.of(2026, 8, 20);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-20T10:00:00Z"),
                ZoneOffset.UTC
        );
        when(projectRepository.findCompletionCandidateIds(
                Set.of(ProjectStatus.AWAITING_REVIEWER, ProjectStatus.ACTIVE),
                expectedDate
        )).thenReturn(List.of(11L, 12L));
        when(completionService.completeIfDue(11L, expectedDate, ISTANBUL))
                .thenThrow(new RuntimeException("lock timeout"));
        ProjectCompletionScheduler scheduler = new ProjectCompletionScheduler(
                projectRepository,
                completionService,
                clock,
                "Europe/Istanbul"
        );

        scheduler.completeDueProjects();

        verify(completionService).completeIfDue(12L, expectedDate, ISTANBUL);
    }
}
