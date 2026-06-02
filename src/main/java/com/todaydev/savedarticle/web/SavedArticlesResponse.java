package com.todaydev.savedarticle.web;

import java.util.List;

public record SavedArticlesResponse(
        List<SavedArticleResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
