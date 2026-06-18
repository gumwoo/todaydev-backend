package com.todaydev.notification.service;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.domain.NotificationPreference;
import com.todaydev.notification.infrastructure.discord.DiscordNotificationSender;
import com.todaydev.notification.infrastructure.email.EmailNotificationPayload;
import com.todaydev.notification.infrastructure.email.EmailNotificationSender;
import com.todaydev.notification.infrastructure.slack.SlackNotificationSender;
import com.todaydev.notification.repository.NotificationPreferenceRepository;
import com.todaydev.notification.web.NotificationPreferenceRequest;
import com.todaydev.notification.web.NotificationPreferenceResponse;
import com.todaydev.preference.web.DeleteResponse;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateService templateService;
    private final EmailNotificationSender emailSender;
    private final SlackNotificationSender slackSender;
    private final DiscordNotificationSender discordSender;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            NotificationTemplateService templateService,
            EmailNotificationSender emailSender,
            SlackNotificationSender slackSender,
            DiscordNotificationSender discordSender
    ) {
        this.preferenceRepository = preferenceRepository;
        this.templateService = templateService;
        this.emailSender = emailSender;
        this.slackSender = slackSender;
        this.discordSender = discordSender;
    }

    public Mono<List<NotificationPreferenceResponse>> findMyPreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(this::toResponse)
                .collectList();
    }

    public Mono<NotificationPreferenceResponse> update(
            Long userId,
            NotificationChannel channel,
            NotificationPreferenceRequest request
    ) {
        String destination = normalizeDestination(channel, request.destination());
        return preferenceRepository.upsert(userId, channel, destination, request.enabled())
                .map(this::toResponse);
    }

    public Mono<DeleteResponse> delete(Long userId, NotificationChannel channel) {
        return preferenceRepository.delete(userId, channel)
                .flatMap(deleted -> deleted
                        ? Mono.just(new DeleteResponse(true))
                        : Mono.error(new TodaydevException(ErrorCode.NOTIFICATION_PREFERENCE_NOT_FOUND)));
    }

    public Mono<Void> sendTest(Long userId, NotificationChannel channel) {
        return preferenceRepository.findByUserIdAndChannel(userId, channel)
                .filter(NotificationPreference::enabled)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.NOTIFICATION_PREFERENCE_NOT_FOUND)))
                .flatMap(preference -> switch (channel) {
                    case EMAIL -> emailSender.send(new EmailNotificationPayload(
                            preference.destination(),
                            templateService.createTestEmailSubject(),
                            templateService.createTestEmailBody()
                    ));
                    case SLACK -> slackSender.send(
                            preference.destination(),
                            templateService.createTestSlackPayload()
                    );
                    case DISCORD -> discordSender.send(
                            preference.destination(),
                            templateService.createTestDiscordPayload()
                    );
                });
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.channel(),
                preference.enabled(),
                preference.destination() != null && !preference.destination().isBlank(),
                preference.updatedAt()
        );
    }

    private String normalizeDestination(NotificationChannel channel, String destination) {
        String normalized = destination == null ? "" : destination.trim();
        if (normalized.isBlank()) {
            throw new TodaydevException(ErrorCode.NOTIFICATION_DESTINATION_INVALID);
        }

        switch (channel) {
            case EMAIL -> {
                if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    throw new TodaydevException(ErrorCode.NOTIFICATION_DESTINATION_INVALID);
                }
            }
            case SLACK, DISCORD -> validateWebhookUrl(normalized);
            default -> throw new TodaydevException(ErrorCode.NOTIFICATION_CHANNEL_UNSUPPORTED);
        }
        return normalized;
    }

    private void validateWebhookUrl(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() == null
                    || (!uri.getScheme().equals("https") && !uri.getScheme().equals("http"))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("Invalid webhook URL");
            }
        } catch (IllegalArgumentException exception) {
            throw new TodaydevException(ErrorCode.NOTIFICATION_DESTINATION_INVALID);
        }
    }
}
