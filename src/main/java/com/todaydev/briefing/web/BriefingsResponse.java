package com.todaydev.briefing.web;

import java.util.List;

public record BriefingsResponse(
        List<BriefingListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
