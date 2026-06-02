package com.todaydev.briefing.service;

import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import com.todaydev.preference.domain.WatchedRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class WatchedRepositoryMatcher {

    public boolean matches(ExternalArticle article, List<WatchedRepository> repositories) {
        if (article == null || article.source() != ExternalSource.GITHUB || repositories == null || repositories.isEmpty()) {
            return false;
        }

        String owner = metadataValue(article, "owner");
        String repoName = metadataValue(article, "repoName");

        if (owner.isBlank() || repoName.isBlank()) {
            return false;
        }

        return repositories.stream()
                .anyMatch(repository -> same(repository.owner(), owner) && same(repository.repoName(), repoName));
    }

    private String metadataValue(ExternalArticle article, String key) {
        Object value = article.metadata().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
