package com.todaydev.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todaydev.common.config.properties.CorsProperties;
import com.todaydev.common.trace.TraceIds;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

class SecurityConfigTest {

    @Test
    void corsConfiguration_usesConfiguredOriginsAndTraceHeader() {
        CorsProperties properties = new CorsProperties(
                List.of("http://localhost:5173", "http://127.0.0.1:5173"),
                List.of("GET", "POST", "OPTIONS"),
                List.of("Authorization", "Content-Type", TraceIds.TRACE_ID_HEADER),
                List.of(TraceIds.TRACE_ID_HEADER),
                true,
                3600
        );

        CorsConfigurationSource source = new SecurityConfig().corsConfigurationSource(properties);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/test")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
        );

        CorsConfiguration configuration = source.getCorsConfiguration(exchange);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("http://localhost:5173", "http://127.0.0.1:5173");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(configuration.getAllowedHeaders()).contains("Authorization", "Content-Type", TraceIds.TRACE_ID_HEADER);
        assertThat(configuration.getExposedHeaders()).containsExactly(TraceIds.TRACE_ID_HEADER);
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getMaxAge()).isEqualTo(3600);
    }

    @Test
    void corsProperties_rejectsWildcardOriginWhenCredentialsAreAllowed() {
        assertThatThrownBy(() -> new CorsProperties(
                List.of("*"),
                List.of("GET", "POST", "OPTIONS"),
                List.of("Authorization", "Content-Type", TraceIds.TRACE_ID_HEADER),
                List.of(TraceIds.TRACE_ID_HEADER),
                true,
                3600
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wildcard origin");
    }
}
