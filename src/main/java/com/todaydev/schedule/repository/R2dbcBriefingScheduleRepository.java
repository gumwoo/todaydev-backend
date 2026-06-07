package com.todaydev.schedule.repository;

import com.todaydev.schedule.domain.BriefingSchedule;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcBriefingScheduleRepository implements BriefingScheduleRepository {

    private final DatabaseClient databaseClient;

    public R2dbcBriefingScheduleRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<BriefingSchedule> createDefault(Long userId) {
        return upsert(
                userId,
                BriefingSchedule.DEFAULT_BRIEFING_TIME,
                BriefingSchedule.DEFAULT_TIMEZONE,
                true
        );
    }

    @Override
    public Mono<BriefingSchedule> findByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT user_id, briefing_time, timezone, enabled, updated_at
                        FROM user_briefing_schedule
                        WHERE user_id = :userId
                        """)
                .bind("userId", userId)
                .map(this::mapSchedule)
                .one();
    }

    @Override
    public Mono<BriefingSchedule> upsert(Long userId, LocalTime briefingTime, String timezone, boolean enabled) {
        return databaseClient.sql("""
                        INSERT INTO user_briefing_schedule (user_id, briefing_time, timezone, enabled)
                        VALUES (:userId, :briefingTime, :timezone, :enabled)
                        ON CONFLICT (user_id)
                        DO UPDATE SET briefing_time = EXCLUDED.briefing_time,
                                      timezone = EXCLUDED.timezone,
                                      enabled = EXCLUDED.enabled,
                                      updated_at = NOW()
                        RETURNING user_id, briefing_time, timezone, enabled, updated_at
                        """)
                .bind("userId", userId)
                .bind("briefingTime", briefingTime)
                .bind("timezone", timezone)
                .bind("enabled", enabled)
                .map(this::mapSchedule)
                .one();
    }

    @Override
    public Flux<BriefingSchedule> findEnabledSchedules() {
        return databaseClient.sql("""
                        SELECT user_id, briefing_time, timezone, enabled, updated_at
                        FROM user_briefing_schedule
                        WHERE enabled = true
                        ORDER BY user_id ASC
                        """)
                .map(this::mapSchedule)
                .all();
    }

    private BriefingSchedule mapSchedule(Row row, RowMetadata metadata) {
        return new BriefingSchedule(
                row.get("user_id", Long.class),
                row.get("briefing_time", LocalTime.class),
                row.get("timezone", String.class),
                Boolean.TRUE.equals(row.get("enabled", Boolean.class)),
                row.get("updated_at", LocalDateTime.class)
        );
    }
}
