package com.todaydev.common.trace;

import java.util.UUID;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

public final class TraceIds {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceIds() {
    }

    public static String from(ServerWebExchange exchange) {
        if (exchange == null) {
            return create();
        }

        ServerHttpRequest request = exchange.getRequest();
        String headerTraceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (hasText(headerTraceId)) {
            return headerTraceId.trim();
        }

        return create();
    }

    private static String create() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
