package com.todaydev.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void success_containsDataAndTimestamp_withoutError() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("ok");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.error()).isNull();
    }

    @Test
    void failure_containsErrorAndTimestamp_withoutData() {
        ApiError error = new ApiError(
                "VALIDATION_FAILED",
                "요청 값이 올바르지 않습니다.",
                List.of(ApiErrorDetail.of("email", "올바른 이메일 형식이 아닙니다.")),
                "trace-1"
        );

        ApiResponse<Void> response = ApiResponse.failure(error);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.error().details()).hasSize(1);
        assertThat(response.error().details().get(0).field()).isEqualTo("email");
        assertThat(response.timestamp()).isNotNull();
    }
}
