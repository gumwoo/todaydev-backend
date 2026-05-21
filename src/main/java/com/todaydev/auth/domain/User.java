package com.todaydev.auth.domain;

import java.time.LocalDateTime;

public record User(
        Long userId,
        String email,
        String passwordHash,
        LocalDateTime createdAt
) {
}
