package com.todaydev.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.briefing.service.BriefingCreationService;
import com.todaydev.schedule.domain.BriefingSchedule;
import com.todaydev.schedule.repository.BriefingScheduleRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.junit.jupiter.api.Test;

class ScheduledBriefingServiceTest {

    private final BriefingScheduleRepository scheduleRepository = mock(BriefingScheduleRepository.class);
    private final BriefingRepository briefingRepository = mock(BriefingRepository.class);
    private final BriefingCreationService briefingCreationService = mock(BriefingCreationService.class);
    private final ScheduledBriefingService service = new ScheduledBriefingService(
            scheduleRepository,
            briefingRepository,
            briefingCreationService
    );

    @Test
    void enqueueDueBriefings_createsBriefingWhenScheduleIsDueAndNotCreatedToday() {
        Instant now = Instant.parse("2026-06-06T23:00:10Z");
        BriefingSchedule schedule = schedule(1L, LocalTime.of(8, 0), "Asia/Seoul");
        Briefing briefing = new Briefing(100L, 1L, null, null, BriefingStatus.GENERATING, LocalDateTime.now());

        when(scheduleRepository.findEnabledSchedules()).thenReturn(Flux.just(schedule));
        when(briefingRepository.existsByUserIdAndGeneratedAtBetween(
                1L,
                LocalDateTime.of(2026, 6, 7, 0, 0),
                LocalDateTime.of(2026, 6, 8, 0, 0)
        )).thenReturn(Mono.just(false));
        when(briefingCreationService.create(1L)).thenReturn(Mono.just(briefing));

        StepVerifier.create(service.enqueueDueBriefings(now))
                .expectNext(1)
                .verifyComplete();

        verify(briefingCreationService).create(1L);
    }

    @Test
    void enqueueDueBriefings_skipsWhenBriefingAlreadyExistsToday() {
        Instant now = Instant.parse("2026-06-06T23:00:10Z");
        BriefingSchedule schedule = schedule(1L, LocalTime.of(8, 0), "Asia/Seoul");

        when(scheduleRepository.findEnabledSchedules()).thenReturn(Flux.just(schedule));
        when(briefingRepository.existsByUserIdAndGeneratedAtBetween(
                1L,
                LocalDateTime.of(2026, 6, 7, 0, 0),
                LocalDateTime.of(2026, 6, 8, 0, 0)
        )).thenReturn(Mono.just(true));

        StepVerifier.create(service.enqueueDueBriefings(now))
                .expectNext(0)
                .verifyComplete();

        verify(briefingCreationService, never()).create(1L);
    }

    @Test
    void isDue_comparesMinuteInUserTimezone() {
        BriefingSchedule schedule = schedule(1L, LocalTime.of(8, 30), "Asia/Seoul");

        assertThat(service.isDue(schedule, Instant.parse("2026-06-06T23:30:59Z"))).isTrue();
        assertThat(service.isDue(schedule, Instant.parse("2026-06-06T23:31:00Z"))).isFalse();
    }

    private BriefingSchedule schedule(Long userId, LocalTime briefingTime, String timezone) {
        return new BriefingSchedule(userId, briefingTime, timezone, true, LocalDateTime.now());
    }
}
