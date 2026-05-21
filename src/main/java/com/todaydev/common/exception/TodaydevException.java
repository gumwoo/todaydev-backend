package com.todaydev.common.exception;

import com.todaydev.common.response.ApiErrorDetail;
import java.util.List;

public class TodaydevException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiErrorDetail> details;

    public TodaydevException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public TodaydevException(ErrorCode errorCode, List<ApiErrorDetail> details) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ApiErrorDetail> details() {
        return details;
    }
}
