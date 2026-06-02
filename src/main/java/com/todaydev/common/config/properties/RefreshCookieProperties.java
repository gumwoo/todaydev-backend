package com.todaydev.common.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.auth.refresh-cookie")
public record RefreshCookieProperties(
        boolean secure,
        boolean httpOnly,
        @NotBlank String sameSite,
        @NotBlank String path
) {

    public RefreshCookieProperties {
        sameSite = sameSite.trim();
        path = path.trim();

        if (!sameSite.equals("Lax") && !sameSite.equals("Strict") && !sameSite.equals("None")) {
            throw new IllegalArgumentException("refresh cookie sameSite must be Lax, Strict, or None");
        }

        if (sameSite.equals("None") && !secure) {
            throw new IllegalArgumentException("refresh cookie SameSite=None requires secure=true");
        }
    }
}
