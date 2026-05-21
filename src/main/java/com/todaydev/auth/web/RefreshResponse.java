package com.todaydev.auth.web;

public record RefreshResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
