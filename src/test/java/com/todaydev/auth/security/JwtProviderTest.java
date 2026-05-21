package com.todaydev.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.auth.domain.User;
import com.todaydev.common.config.properties.JwtProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtProviderTest {

    private static final JwtProperties PROPERTIES = new JwtProperties(
            "test-secret-key-must-be-at-least-32-characters",
            1800,
            1209600
    );

    private final JwtProvider jwtProvider = new JwtProvider(
            PROPERTIES,
            new ObjectMapper(),
            Clock.fixed(Instant.parse("2026-05-21T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void createAccessToken_canBeParsed() {
        User user = new User(1L, "user@example.com", "hash", LocalDateTime.now());

        String token = jwtProvider.createAccessToken(user);
        JwtClaims claims = jwtProvider.parseAccessToken(token);

        assertThat(claims.userId()).isEqualTo(1L);
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2026-05-21T00:30:00Z"));
    }

    @Test
    void parseAccessToken_rejectsTamperedToken() {
        User user = new User(1L, "user@example.com", "hash", LocalDateTime.now());
        String token = jwtProvider.createAccessToken(user);
        String tamperedToken = token.substring(0, token.length() - 2) + "xx";

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(tamperedToken))
                .isInstanceOf(TodaydevException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    void parseAccessToken_rejectsRefreshToken() {
        User user = new User(1L, "user@example.com", "hash", LocalDateTime.now());
        String refreshToken = jwtProvider.createRefreshToken(user);

        assertThatThrownBy(() -> jwtProvider.parseAccessToken(refreshToken))
                .isInstanceOf(TodaydevException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
    }
}
