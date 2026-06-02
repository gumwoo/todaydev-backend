package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.MatchedKeyword;
import com.todaydev.external.ExternalArticle;
import com.todaydev.preference.domain.InterestKeyword;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class KeywordMatcher {

    public List<MatchedKeyword> match(ExternalArticle article, List<InterestKeyword> keywords) {
        if (article == null || keywords == null || keywords.isEmpty()) {
            return List.of();
        }

        String haystack = searchableText(article);

        return keywords.stream()
                .filter(keyword -> containsKeyword(haystack, keyword.keyword()))
                .map(keyword -> new MatchedKeyword(keyword.keyword(), keyword.weight()))
                .toList();
    }

    private String searchableText(ExternalArticle article) {
        return String.join(" ",
                        safe(article.title()),
                        safe(article.summary()),
                        safe(article.url()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsKeyword(String haystack, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        return haystack.contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
