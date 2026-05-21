package com.todaydev.common.response;

import java.util.List;

public record ApiError(
        String code,
        String message,
        List<ApiErrorDetail> details,
        String traceId
) {

    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
