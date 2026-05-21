package com.todaydev.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.response.ApiError;
import com.todaydev.common.response.ApiResponse;
import com.todaydev.common.trace.TraceIds;
import java.util.List;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange, ErrorCode errorCode) {
        String traceId = TraceIds.from(exchange);
        ApiError error = new ApiError(
                errorCode.name(),
                errorCode.message(),
                List.of(),
                traceId
        );
        ApiResponse<Void> body = ApiResponse.failure(error);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception exception) {
            bytes = fallbackBody(errorCode, traceId).getBytes();
        }

        exchange.getResponse().setStatusCode(HttpStatus.valueOf(errorCode.httpStatus().value()));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set(TraceIds.TRACE_ID_HEADER, traceId);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String fallbackBody(ErrorCode errorCode, String traceId) {
        return """
                {"success":false,"error":{"code":"%s","message":"%s","details":[],"traceId":"%s"}}
                """.formatted(errorCode.name(), errorCode.message(), traceId);
    }
}
