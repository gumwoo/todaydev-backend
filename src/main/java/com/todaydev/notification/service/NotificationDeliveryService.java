package com.todaydev.notification.service;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.repository.BriefingItemDetail;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.notification.domain.NotificationDeliveryStatus;
import com.todaydev.notification.domain.NotificationPreference;
import com.todaydev.notification.infrastructure.discord.DiscordNotificationSender;
import com.todaydev.notification.infrastructure.email.EmailNotificationSender;
import com.todaydev.notification.infrastructure.queue.NotificationDeadLetterPublisher;
import com.todaydev.notification.infrastructure.queue.NotificationPublisher;
import com.todaydev.notification.infrastructure.queue.NotificationQueueMessage;
import com.todaydev.notification.infrastructure.slack.SlackNotificationSender;
import com.todaydev.notification.repository.NotificationDeliveryRepository;
import com.todaydev.notification.repository.NotificationPreferenceRepository;
import com.todaydev.notification.web.NotificationDeliveriesResponse;
import com.todaydev.notification.web.NotificationDeliveryResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NotificationDeliveryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final BriefingRepository briefingRepository;
    private final NotificationTemplateService templateService;
    private final EmailNotificationSender emailSender;
    private final SlackNotificationSender slackSender;
    private final DiscordNotificationSender discordSender;
    private final NotificationRetryPolicy retryPolicy;
    private final NotificationMessageFactory messageFactory;
    private final NotificationPublisher publisher;
    private final NotificationDeadLetterPublisher deadLetterPublisher;

    public NotificationDeliveryService(
            NotificationDeliveryRepository deliveryRepository,
            NotificationPreferenceRepository preferenceRepository,
            BriefingRepository briefingRepository,
            NotificationTemplateService templateService,
            EmailNotificationSender emailSender,
            SlackNotificationSender slackSender,
            DiscordNotificationSender discordSender,
            NotificationRetryPolicy retryPolicy,
            NotificationMessageFactory messageFactory,
            NotificationPublisher publisher,
            NotificationDeadLetterPublisher deadLetterPublisher
    ) {
        this.deliveryRepository = deliveryRepository;
        this.preferenceRepository = preferenceRepository;
        this.briefingRepository = briefingRepository;
        this.templateService = templateService;
        this.emailSender = emailSender;
        this.slackSender = slackSender;
        this.discordSender = discordSender;
        this.retryPolicy = retryPolicy;
        this.messageFactory = messageFactory;
        this.publisher = publisher;
        this.deadLetterPublisher = deadLetterPublisher;
    }

    public Mono<NotificationDeliveriesResponse> findMyDeliveries(Long userId, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        Mono<List<NotificationDeliveryResponse>> items = deliveryRepository
                .findByUserId(userId, normalizedPage, normalizedSize)
                .map(delivery -> new NotificationDeliveryResponse(
                        delivery.deliveryId(),
                        delivery.briefingId(),
                        delivery.channel(),
                        delivery.status(),
                        delivery.attemptCount(),
                        delivery.queuedAt(),
                        delivery.sentAt(),
                        delivery.updatedAt()
                ))
                .collectList();
        Mono<Long> total = deliveryRepository.countByUserId(userId);

        return Mono.zip(items, total)
                .map(tuple -> new NotificationDeliveriesResponse(
                        tuple.getT1(),
                        normalizedPage,
                        normalizedSize,
                        tuple.getT2(),
                        totalPages(tuple.getT2(), normalizedSize),
                        normalizedPage + 1 < totalPages(tuple.getT2(), normalizedSize)
                ));
    }

    public Mono<Void> process(NotificationQueueMessage message) {
        return deliveryRepository.findById(message.deliveryId())
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.NOTIFICATION_DELIVERY_NOT_FOUND)))
                .flatMap(delivery -> {
                    if (delivery.status() == NotificationDeliveryStatus.SENT) {
                        return Mono.empty();
                    }
                    return deliveryRepository.markSending(delivery.deliveryId(), message.attempt())
                            .then(send(message))
                            .then(deliveryRepository.markSent(delivery.deliveryId(), message.attempt()))
                            .then();
                })
                .onErrorResume(error -> handleFailure(message, error));
    }

    private Mono<Void> send(NotificationQueueMessage message) {
        Mono<NotificationPreference> preference = preferenceRepository
                .findByUserIdAndChannel(message.userId(), message.channel())
                .filter(NotificationPreference::enabled)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.NOTIFICATION_PREFERENCE_NOT_FOUND)));

        Mono<Briefing> briefing = briefingRepository
                .findByIdAndUserId(message.briefingId(), message.userId())
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.BRIEFING_NOT_FOUND)));

        Mono<List<BriefingItemDetail>> items = briefingRepository
                .findItemsByBriefingIdAndUserId(message.briefingId(), message.userId())
                .collectList();

        return Mono.zip(preference, briefing, items)
                .flatMap(tuple -> switch (message.channel()) {
                    case EMAIL -> emailSender.send(templateService.createBriefingEmail(
                            tuple.getT1().destination(),
                            tuple.getT2(),
                            tuple.getT3()
                    ));
                    case SLACK -> slackSender.send(
                            tuple.getT1().destination(),
                            templateService.createBriefingSlackPayload(tuple.getT2(), tuple.getT3())
                    );
                    case DISCORD -> discordSender.send(
                            tuple.getT1().destination(),
                            templateService.createBriefingDiscordPayload(tuple.getT2(), tuple.getT3())
                    );
                });
    }

    private Mono<Void> handleFailure(NotificationQueueMessage message, Throwable error) {
        String errorCode = error instanceof TodaydevException exception
                ? exception.errorCode().name()
                : ErrorCode.NOTIFICATION_SEND_FAILED.name();
        String errorMessage = error.getMessage();

        if (retryPolicy.canRetry(message.attempt(), error)) {
            NotificationQueueMessage retryMessage = nextAttempt(message);
            return deliveryRepository.updateStatus(
                            message.deliveryId(),
                            NotificationDeliveryStatus.RETRYING,
                            message.attempt(),
                            errorCode,
                            errorMessage
                    )
                    .then(publisher.publishRetry(retryMessage))
                    .then();
        }

        return deliveryRepository.updateStatus(
                        message.deliveryId(),
                        NotificationDeliveryStatus.DLQ,
                        message.attempt(),
                        errorCode,
                        errorMessage
                )
                .then(deadLetterPublisher.publish(message, error))
                .then();
    }

    private NotificationQueueMessage nextAttempt(NotificationQueueMessage message) {
        return messageFactory.toQueueMessage(messageFactory.create(
                new com.todaydev.notification.domain.NotificationDelivery(
                        message.deliveryId(),
                        message.userId(),
                        message.briefingId(),
                        message.channel(),
                        NotificationDeliveryStatus.RETRYING,
                        message.attempt(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                message.attempt() + 1
        ));
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private int totalPages(long totalElements, int size) {
        return totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }
}
