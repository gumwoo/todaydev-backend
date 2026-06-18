package com.todaydev.notification.service;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.notification.domain.NotificationDeliveryStatus;
import com.todaydev.notification.infrastructure.queue.NotificationPublisher;
import com.todaydev.notification.repository.NotificationDeliveryRepository;
import com.todaydev.notification.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class NotificationEnqueueService {

    private static final Logger log = LoggerFactory.getLogger(NotificationEnqueueService.class);

    private final NotificationProperties properties;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationMessageFactory messageFactory;
    private final NotificationPublisher publisher;

    public NotificationEnqueueService(
            NotificationProperties properties,
            NotificationPreferenceRepository preferenceRepository,
            NotificationDeliveryRepository deliveryRepository,
            NotificationMessageFactory messageFactory,
            NotificationPublisher publisher
    ) {
        this.properties = properties;
        this.preferenceRepository = preferenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.messageFactory = messageFactory;
        this.publisher = publisher;
    }

    public Mono<Void> enqueueForBriefing(Briefing briefing) {
        if (!properties.enabled() || briefing.status() == BriefingStatus.FAILED) {
            return Mono.empty();
        }

        return preferenceRepository.findEnabledByUserId(briefing.userId())
                .flatMap(preference -> deliveryRepository
                        .createPending(briefing.userId(), briefing.briefingId(), preference.channel())
                        .flatMap(delivery -> publisher
                                .publish(messageFactory.toQueueMessage(messageFactory.create(delivery, 1)))
                                .then(deliveryRepository.markPublished(delivery.deliveryId()))
                                .onErrorResume(error -> deliveryRepository.updateStatus(
                                                delivery.deliveryId(),
                                                NotificationDeliveryStatus.FAILED,
                                                delivery.attemptCount(),
                                                ErrorCode.NOTIFICATION_PUBLISH_FAILED.name(),
                                                error.getMessage()
                                        )
                                        .doOnNext(failed -> log.warn(
                                                "Notification publish failed: briefingId={}, deliveryId={}, channel={}",
                                                briefing.briefingId(), delivery.deliveryId(), preference.channel(), error
                                        ))
                                        .then(Mono.empty()))))
                .then();
    }
}
