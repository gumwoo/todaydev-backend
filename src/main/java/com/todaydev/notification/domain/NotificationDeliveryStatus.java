package com.todaydev.notification.domain;

public enum NotificationDeliveryStatus {
    PENDING,
    PUBLISHED,
    SENDING,
    SENT,
    RETRYING,
    FAILED,
    DLQ,
    SKIPPED
}
