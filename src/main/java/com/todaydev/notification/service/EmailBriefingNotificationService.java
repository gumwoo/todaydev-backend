package com.todaydev.notification.service;

import com.todaydev.auth.repository.UserRepository;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.notification.infrastructure.email.EmailNotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class EmailBriefingNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailBriefingNotificationService.class);

    private final UserRepository userRepository;
    private final BriefingRepository briefingRepository;
    private final NotificationTemplateService templateService;
    private final EmailNotificationSender emailSender;
    private final NotificationProperties properties;

    public EmailBriefingNotificationService(
            UserRepository userRepository,
            BriefingRepository briefingRepository,
            NotificationTemplateService templateService,
            EmailNotificationSender emailSender,
            NotificationProperties properties
    ) {
        this.userRepository = userRepository;
        this.briefingRepository = briefingRepository;
        this.templateService = templateService;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    public Mono<Void> sendBriefingResult(Briefing briefing) {
        if (!properties.enabled() || !properties.email().enabled() || briefing.status() == BriefingStatus.FAILED) {
            return Mono.empty();
        }

        return userRepository.findById(briefing.userId())
                .zipWith(briefingRepository.findItemsByBriefingIdAndUserId(briefing.briefingId(), briefing.userId())
                        .collectList())
                .map(result -> templateService.createBriefingEmail(result.getT1().email(), briefing, result.getT2()))
                .flatMap(emailSender::send)
                .doOnSuccess(ignored -> log.info(
                        "Briefing email sent: briefingId={}, userId={}, status={}",
                        briefing.briefingId(), briefing.userId(), briefing.status()
                ))
                .doOnError(error -> log.warn(
                        "Briefing email send failed: briefingId={}, userId={}, status={}",
                        briefing.briefingId(), briefing.userId(), briefing.status(), error
                ))
                .onErrorResume(error -> Mono.empty());
    }
}
