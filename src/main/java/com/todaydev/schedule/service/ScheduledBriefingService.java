package com.todaydev.schedule.service;

import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.briefing.service.BriefingCreationService;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.schedule.domain.BriefingSchedule;
import com.todaydev.schedule.repository.BriefingScheduleRepository;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ScheduledBriefingService {

    private final BriefingScheduleRepository scheduleRepository;
    private final BriefingRepository briefingRepository;
    private final BriefingCreationService briefingCreationService;

    public ScheduledBriefingService(
            BriefingScheduleRepository scheduleRepository,
            BriefingRepository briefingRepository,
            BriefingCreationService briefingCreationService
    ) {
        this.scheduleRepository = scheduleRepository;
        this.briefingRepository = briefingRepository;
        this.briefingCreationService = briefingCreationService;
    }

    public Mono<Integer> enqueueDueBriefings(Instant now) {
        return scheduleRepository.findEnabledSchedules()
                .filter(schedule -> isDue(schedule, now))
                .flatMap(schedule -> enqueueIfNotCreatedToday(schedule, now))
                .filter(Boolean::booleanValue)
                .count()
                .map(Long::intValue);
    }

    boolean isDue(BriefingSchedule schedule, Instant now) {
        ZoneId zoneId = zoneId(schedule.timezone());
        LocalTime localTime = now.atZone(zoneId).toLocalTime();

        return localTime.getHour() == schedule.briefingTime().getHour()
                && localTime.getMinute() == schedule.briefingTime().getMinute();
    }

    private Mono<Boolean> enqueueIfNotCreatedToday(BriefingSchedule schedule, Instant now) {
        ZoneId zoneId = zoneId(schedule.timezone());
        LocalDate localDate = now.atZone(zoneId).toLocalDate();
        LocalDateTime start = localDate.atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return briefingRepository.existsByUserIdAndGeneratedAtBetween(schedule.userId(), start, end)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(false);
                    }

                    return briefingCreationService.create(schedule.userId())
                            .thenReturn(true)
                            .onErrorResume(TodaydevException.class, this::handleCreateError);
                });
    }

    private Mono<Boolean> handleCreateError(TodaydevException exception) {
        if (exception.errorCode() == ErrorCode.BRIEFING_ALREADY_IN_PROGRESS) {
            return Mono.just(false);
        }

        return Mono.error(exception);
    }

    private ZoneId zoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneId.of(BriefingSchedule.DEFAULT_TIMEZONE);
        }
    }
}
