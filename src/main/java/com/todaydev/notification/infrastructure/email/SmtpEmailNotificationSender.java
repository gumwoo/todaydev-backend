package com.todaydev.notification.infrastructure.email;

import com.todaydev.common.config.properties.NotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class SmtpEmailNotificationSender implements EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public SmtpEmailNotificationSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public Mono<Void> send(EmailNotificationPayload payload) {
        return Mono.fromRunnable(() -> sendBlocking(payload))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    private void sendBlocking(EmailNotificationPayload payload) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(properties.email().from());
            helper.setTo(payload.to());
            helper.setSubject(payload.subject());
            helper.setText(payload.body(), true);
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Failed to create briefing email message", exception);
        }
    }
}
