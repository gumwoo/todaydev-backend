package com.todaydev.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todaydev.auth.service.AuthResult;
import com.todaydev.common.config.properties.RefreshCookieProperties;
import org.junit.jupiter.api.Test;

class RefreshCookieFactoryTest {

    @Test
    void create_usesConfiguredSecureHttpOnlySameSiteAndPath() {
        RefreshCookieFactory factory = new RefreshCookieFactory(
                new RefreshCookieProperties(true, true, "None", "/api/auth")
        );
        AuthResult result = new AuthResult(null, "refresh-token-value", 120);

        String cookie = factory.create(result).toString();

        assertThat(cookie).contains("refreshToken=refresh-token-value");
        assertThat(cookie).contains("Path=/api/auth");
        assertThat(cookie).contains("Max-Age=120");
        assertThat(cookie).contains("Secure");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=None");
    }

    @Test
    void expire_clearsRefreshCookieWithConfiguredAttributes() {
        RefreshCookieFactory factory = new RefreshCookieFactory(
                new RefreshCookieProperties(false, true, "Lax", "/api/auth")
        );

        String cookie = factory.expire().toString();

        assertThat(cookie).contains("refreshToken=");
        assertThat(cookie).contains("Path=/api/auth");
        assertThat(cookie).contains("Max-Age=0");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("SameSite=Lax");
        assertThat(cookie).doesNotContain("Secure");
    }

    @Test
    void sameSiteNone_requiresSecureCookie() {
        assertThatThrownBy(() -> new RefreshCookieProperties(false, true, "None", "/api/auth"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("secure=true");
    }
}
