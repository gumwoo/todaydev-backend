package com.todaydev.notification.service;

import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetryPolicy {

    private final NotificationProperties properties;

    public NotificationRetryPolicy(NotificationProperties properties) {
        this.properties = properties;
    }

    public boolean canRetry(int attempt, Throwable throwable) {
        return attempt < properties.queue().retryMaxAttempts() && isRetryable(throwable);
    }

    public Duration backoff(int attempt) {
        long multiplier = Math.max(1, attempt);
        return Duration.ofMillis(properties.queue().retryBackoffMillis() * multiplier);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof TodaydevException exception) {
            ErrorCode errorCode = exception.errorCode();
            return errorCode == ErrorCode.NOTIFICATION_SEND_FAILED
                    || errorCode == ErrorCode.NOTIFICATION_PUBLISH_FAILED
                    || errorCode == ErrorCode.NOTIFICATION_RATE_LIMITED;
        }
        return true;
    }
}
