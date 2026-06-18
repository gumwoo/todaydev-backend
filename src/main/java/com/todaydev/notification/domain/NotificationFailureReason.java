package com.todaydev.notification.domain;

public enum NotificationFailureReason {
    PUBLISH_FAILED,
    SEND_FAILED,
    RATE_LIMITED,
    TIMEOUT,
    DLQ_PUBLISHED
}
