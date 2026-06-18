package com.todaydev.notification.web;

import java.util.List;

public record NotificationDeliveriesResponse(
        List<NotificationDeliveryResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
