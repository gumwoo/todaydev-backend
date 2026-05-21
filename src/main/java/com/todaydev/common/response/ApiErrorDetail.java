package com.todaydev.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorDetail(
        String field,
        String reason
) {

    public static ApiErrorDetail of(String field, String reason) {
        return new ApiErrorDetail(field, reason);
    }

    public static ApiErrorDetail global(String reason) {
        return new ApiErrorDetail(null, reason);
    }
}
