package com.todaydev.notification.web;

import jakarta.validation.constraints.NotBlank;

public record NotificationPreferenceRequest(
        @NotBlank String destination,
        boolean enabled
) {
}
