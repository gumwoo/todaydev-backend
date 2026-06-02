package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.Source;
import com.todaydev.external.ExternalArticle;
import java.util.List;

public record SourceCollectionResult(
        Source source,
        List<ExternalArticle> articles,
        ApiCallLog apiCallLog
) {

    public SourceCollectionResult {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }

    public boolean success() {
        return apiCallLog != null && apiCallLog.status().name().equals("SUCCESS");
    }
}
