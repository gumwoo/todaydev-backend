package com.todaydev.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.common.response.ApiResponse;
import com.todaydev.common.trace.TraceIds;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleTodaydevException_returnsContractErrorResponse() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test")
                        .header(TraceIds.TRACE_ID_HEADER, "trace-123")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleTodaydevException(
                new TodaydevException(ErrorCode.RESOURCE_NOT_FOUND),
                exchange
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getFirst(TraceIds.TRACE_ID_HEADER)).isEqualTo("trace-123");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().error().message()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
        assertThat(response.getBody().error().traceId()).isEqualTo("trace-123");
    }

    @Test
    void handleThrowable_hidesRawExceptionMessage() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/test")
                        .header(TraceIds.TRACE_ID_HEADER, "trace-456")
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleThrowable(
                new IllegalStateException("database password leaked"),
                exchange
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(response.getBody().error().message()).doesNotContain("database password leaked");
    }
}
