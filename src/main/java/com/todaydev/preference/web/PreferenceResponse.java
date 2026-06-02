package com.todaydev.preference.web;

import java.util.List;

public record PreferenceResponse(
        List<KeywordResponse> keywords,
        List<RepositoryResponse> repositories
) {
}
