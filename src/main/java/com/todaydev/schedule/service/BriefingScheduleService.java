package com.todaydev.schedule.service;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.schedule.domain.BriefingSchedule;
import com.todaydev.schedule.repository.BriefingScheduleRepository;
import com.todaydev.schedule.web.BriefingScheduleRequest;
import com.todaydev.schedule.web.BriefingScheduleResponse;
import java.time.DateTimeException;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BriefingScheduleService {

    private final BriefingScheduleRepository scheduleRepository;

    public BriefingScheduleService(BriefingScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public Mono<BriefingScheduleResponse> getMySchedule(Long userId) {
        return scheduleRepository.findByUserId(userId)
                .switchIfEmpty(scheduleRepository.createDefault(userId))
                .map(this::toResponse);
    }

    public Mono<BriefingScheduleResponse> updateMySchedule(Long userId, BriefingScheduleRequest request) {
        String timezone = normalizeTimezone(request.timezone());

        return scheduleRepository.upsert(userId, request.briefingTime(), timezone, request.enabled())
                .map(this::toResponse);
    }

    private String normalizeTimezone(String timezone) {
        String normalized = timezone.trim();

        try {
            return ZoneId.of(normalized).getId();
        } catch (DateTimeException exception) {
            throw new TodaydevException(ErrorCode.INVALID_REQUEST);
        }
    }

    private BriefingScheduleResponse toResponse(BriefingSchedule schedule) {
        return new BriefingScheduleResponse(
                schedule.briefingTime(),
                schedule.timezone(),
                schedule.enabled(),
                schedule.updatedAt()
        );
    }
}
