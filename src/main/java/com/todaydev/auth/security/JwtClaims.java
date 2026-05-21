package com.todaydev.auth.security;

import java.time.Instant;

public record JwtClaims(
        Long userId,
        String email,
        Instant expiresAt
) {
}
