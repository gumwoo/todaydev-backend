package com.todaydev.briefing.domain;

public record ApiCallLog(
        Long briefingId,
        Source source,
        ApiCallStatus status,
        Integer latencyMs,
        String errorMessage
) {

    public static ApiCallLog success(Long briefingId, Source source, long latencyMs) {
        return new ApiCallLog(briefingId, source, ApiCallStatus.SUCCESS, safeLatency(latencyMs), null);
    }

    public static ApiCallLog failed(Long briefingId, Source source, long latencyMs) {
        return new ApiCallLog(briefingId, source, ApiCallStatus.FAILED, safeLatency(latencyMs), "External source failed");
    }

    private static int safeLatency(long latencyMs) {
        return latencyMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) latencyMs;
    }
}
