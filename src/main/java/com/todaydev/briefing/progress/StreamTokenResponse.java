package com.todaydev.briefing.progress;

public record StreamTokenResponse(
        String streamToken,
        long expiresIn
) {
}
