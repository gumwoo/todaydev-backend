package com.todaydev.notification.repository;

import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.domain.NotificationPreference;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcNotificationPreferenceRepository implements NotificationPreferenceRepository {

    private final DatabaseClient databaseClient;

    public R2dbcNotificationPreferenceRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<NotificationPreference> findByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT preference_id, user_id, channel, destination, enabled, created_at, updated_at
                        FROM notification_preference
                        WHERE user_id = :userId
                        ORDER BY channel
                        """)
                .bind("userId", userId)
                .map(this::mapPreference)
                .all();
    }

    @Override
    public Flux<NotificationPreference> findEnabledByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT preference_id, user_id, channel, destination, enabled, created_at, updated_at
                        FROM notification_preference
                        WHERE user_id = :userId
                          AND enabled = TRUE
                        ORDER BY channel
                        """)
                .bind("userId", userId)
                .map(this::mapPreference)
                .all();
    }

    @Override
    public Mono<NotificationPreference> findByUserIdAndChannel(Long userId, NotificationChannel channel) {
        return databaseClient.sql("""
                        SELECT preference_id, user_id, channel, destination, enabled, created_at, updated_at
                        FROM notification_preference
                        WHERE user_id = :userId
                          AND channel = :channel
                        """)
                .bind("userId", userId)
                .bind("channel", channel.name())
                .map(this::mapPreference)
                .one();
    }

    @Override
    public Mono<NotificationPreference> upsert(Long userId, NotificationChannel channel, String destination, boolean enabled) {
        return databaseClient.sql("""
                        INSERT INTO notification_preference (user_id, channel, destination, enabled)
                        VALUES (:userId, :channel, :destination, :enabled)
                        ON CONFLICT (user_id, channel)
                        DO UPDATE SET destination = EXCLUDED.destination,
                                      enabled = EXCLUDED.enabled,
                                      updated_at = NOW()
                        RETURNING preference_id, user_id, channel, destination, enabled, created_at, updated_at
                        """)
                .bind("userId", userId)
                .bind("channel", channel.name())
                .bind("destination", destination)
                .bind("enabled", enabled)
                .map(this::mapPreference)
                .one();
    }

    @Override
    public Mono<Boolean> delete(Long userId, NotificationChannel channel) {
        return databaseClient.sql("""
                        DELETE FROM notification_preference
                        WHERE user_id = :userId
                          AND channel = :channel
                        """)
                .bind("userId", userId)
                .bind("channel", channel.name())
                .fetch()
                .rowsUpdated()
                .map(count -> count > 0);
    }

    private NotificationPreference mapPreference(Row row, RowMetadata metadata) {
        return new NotificationPreference(
                row.get("preference_id", Long.class),
                row.get("user_id", Long.class),
                NotificationChannel.valueOf(row.get("channel", String.class)),
                row.get("destination", String.class),
                Boolean.TRUE.equals(row.get("enabled", Boolean.class)),
                row.get("created_at", LocalDateTime.class),
                row.get("updated_at", LocalDateTime.class)
        );
    }
}
