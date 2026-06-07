package com.todaydev.schedule.repository;

import com.todaydev.schedule.domain.BriefingSchedule;
import java.time.LocalTime;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BriefingScheduleRepository {

    Mono<BriefingSchedule> createDefault(Long userId);

    Mono<BriefingSchedule> findByUserId(Long userId);

    Mono<BriefingSchedule> upsert(Long userId, LocalTime briefingTime, String timezone, boolean enabled);

    Flux<BriefingSchedule> findEnabledSchedules();
}
