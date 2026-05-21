package com.todaydev.auth.domain;

public record AuthenticatedUser(
        Long userId,
        String email
) {
}
