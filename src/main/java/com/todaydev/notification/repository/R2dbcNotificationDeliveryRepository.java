package com.todaydev.notification.repository;

import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.domain.NotificationDelivery;
import com.todaydev.notification.domain.NotificationDeliveryStatus;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcNotificationDeliveryRepository implements NotificationDeliveryRepository {

    private final DatabaseClient databaseClient;

    public R2dbcNotificationDeliveryRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<NotificationDelivery> createPending(Long userId, Long briefingId, NotificationChannel channel) {
        return databaseClient.sql("""
                        INSERT INTO notification_delivery (user_id, briefing_id, channel, status, queued_at)
                        VALUES (:userId, :briefingId, :channel, :status, NOW())
                        ON CONFLICT (user_id, briefing_id, channel)
                        DO UPDATE SET status = EXCLUDED.status,
                                      attempt_count = 0,
                                      last_error_code = NULL,
                                      last_error_message = NULL,
                                      queued_at = NOW(),
                                      sent_at = NULL,
                                      updated_at = NOW()
                        RETURNING delivery_id, user_id, briefing_id, channel, status, attempt_count,
                                  last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        """)
                .bind("userId", userId)
                .bind("briefingId", briefingId)
                .bind("channel", channel.name())
                .bind("status", NotificationDeliveryStatus.PENDING.name())
                .map(this::mapDelivery)
                .one();
    }

    @Override
    public Mono<NotificationDelivery> findById(Long deliveryId) {
        return databaseClient.sql("""
                        SELECT delivery_id, user_id, briefing_id, channel, status, attempt_count,
                               last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        FROM notification_delivery
                        WHERE delivery_id = :deliveryId
                        """)
                .bind("deliveryId", deliveryId)
                .map(this::mapDelivery)
                .one();
    }

    @Override
    public Flux<NotificationDelivery> findByUserId(Long userId, int page, int size) {
        return databaseClient.sql("""
                        SELECT delivery_id, user_id, briefing_id, channel, status, attempt_count,
                               last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        FROM notification_delivery
                        WHERE user_id = :userId
                        ORDER BY created_at DESC, delivery_id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .bind("userId", userId)
                .bind("limit", size)
                .bind("offset", page * size)
                .map(this::mapDelivery)
                .all();
    }

    @Override
    public Mono<Long> countByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) AS total_count
                        FROM notification_delivery
                        WHERE user_id = :userId
                        """)
                .bind("userId", userId)
                .map(row -> row.get("total_count", Long.class))
                .one();
    }

    @Override
    public Mono<NotificationDelivery> updateStatus(
            Long deliveryId,
            NotificationDeliveryStatus status,
            int attemptCount,
            String errorCode,
            String errorMessage
    ) {
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql("""
                        UPDATE notification_delivery
                        SET status = :status,
                            attempt_count = :attemptCount,
                            last_error_code = :errorCode,
                            last_error_message = :errorMessage,
                            updated_at = NOW()
                        WHERE delivery_id = :deliveryId
                        RETURNING delivery_id, user_id, briefing_id, channel, status, attempt_count,
                                  last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        """)
                .bind("deliveryId", deliveryId)
                .bind("status", status.name())
                .bind("attemptCount", attemptCount);
        spec = bindNullable(spec, "errorCode", errorCode, String.class);
        spec = bindNullable(spec, "errorMessage", errorMessage, String.class);
        return spec.map(this::mapDelivery).one();
    }

    @Override
    public Mono<NotificationDelivery> markPublished(Long deliveryId) {
        return simpleStatus(deliveryId, NotificationDeliveryStatus.PUBLISHED);
    }

    @Override
    public Mono<NotificationDelivery> markSending(Long deliveryId, int attemptCount) {
        return databaseClient.sql("""
                        UPDATE notification_delivery
                        SET status = :status,
                            attempt_count = :attemptCount,
                            updated_at = NOW()
                        WHERE delivery_id = :deliveryId
                        RETURNING delivery_id, user_id, briefing_id, channel, status, attempt_count,
                                  last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        """)
                .bind("deliveryId", deliveryId)
                .bind("status", NotificationDeliveryStatus.SENDING.name())
                .bind("attemptCount", attemptCount)
                .map(this::mapDelivery)
                .one();
    }

    @Override
    public Mono<NotificationDelivery> markSent(Long deliveryId, int attemptCount) {
        return databaseClient.sql("""
                        UPDATE notification_delivery
                        SET status = :status,
                            attempt_count = :attemptCount,
                            sent_at = NOW(),
                            last_error_code = NULL,
                            last_error_message = NULL,
                            updated_at = NOW()
                        WHERE delivery_id = :deliveryId
                        RETURNING delivery_id, user_id, briefing_id, channel, status, attempt_count,
                                  last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        """)
                .bind("deliveryId", deliveryId)
                .bind("status", NotificationDeliveryStatus.SENT.name())
                .bind("attemptCount", attemptCount)
                .map(this::mapDelivery)
                .one();
    }

    private Mono<NotificationDelivery> simpleStatus(Long deliveryId, NotificationDeliveryStatus status) {
        return databaseClient.sql("""
                        UPDATE notification_delivery
                        SET status = :status,
                            updated_at = NOW()
                        WHERE delivery_id = :deliveryId
                        RETURNING delivery_id, user_id, briefing_id, channel, status, attempt_count,
                                  last_error_code, last_error_message, queued_at, sent_at, created_at, updated_at
                        """)
                .bind("deliveryId", deliveryId)
                .bind("status", status.name())
                .map(this::mapDelivery)
                .one();
    }

    private DatabaseClient.GenericExecuteSpec bindNullable(
            DatabaseClient.GenericExecuteSpec spec,
            String name,
            String value,
            Class<String> type
    ) {
        return value == null ? spec.bindNull(name, type) : spec.bind(name, value);
    }

    private NotificationDelivery mapDelivery(Row row, RowMetadata metadata) {
        return new NotificationDelivery(
                row.get("delivery_id", Long.class),
                row.get("user_id", Long.class),
                row.get("briefing_id", Long.class),
                NotificationChannel.valueOf(row.get("channel", String.class)),
                NotificationDeliveryStatus.valueOf(row.get("status", String.class)),
                row.get("attempt_count", Integer.class),
                row.get("last_error_code", String.class),
                row.get("last_error_message", String.class),
                row.get("queued_at", LocalDateTime.class),
                row.get("sent_at", LocalDateTime.class),
                row.get("created_at", LocalDateTime.class),
                row.get("updated_at", LocalDateTime.class)
        );
    }
}
