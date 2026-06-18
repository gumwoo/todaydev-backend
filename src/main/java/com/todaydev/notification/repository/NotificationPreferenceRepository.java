package com.todaydev.notification.repository;

import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.domain.NotificationPreference;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface NotificationPreferenceRepository {

    Flux<NotificationPreference> findByUserId(Long userId);

    Flux<NotificationPreference> findEnabledByUserId(Long userId);

    Mono<NotificationPreference> findByUserIdAndChannel(Long userId, NotificationChannel channel);

    Mono<NotificationPreference> upsert(Long userId, NotificationChannel channel, String destination, boolean enabled);

    Mono<Boolean> delete(Long userId, NotificationChannel channel);
}
