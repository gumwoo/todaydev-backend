package com.todaydev.auth.web;

import com.todaydev.auth.security.AuthCookies;
import com.todaydev.auth.service.AuthResult;
import com.todaydev.common.config.properties.RefreshCookieProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieFactory {

    private final RefreshCookieProperties properties;

    public RefreshCookieFactory(RefreshCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(AuthResult result) {
        return baseCookie(result.refreshToken())
                .maxAge(Duration.ofSeconds(result.refreshTokenMaxAge()))
                .build();
    }

    public ResponseCookie expire() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(AuthCookies.REFRESH_TOKEN, value)
                .httpOnly(properties.httpOnly())
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path());
    }
}
