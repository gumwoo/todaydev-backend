package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.BriefingCandidate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BriefingDeduplicator {

    public List<BriefingCandidate> deduplicate(Collection<BriefingCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, BriefingCandidate> bestByKey = new LinkedHashMap<>();

        candidates.forEach(candidate -> bestByKey.merge(
                dedupeKey(candidate),
                candidate,
                this::higherScore
        ));

        return bestByKey.values()
                .stream()
                .sorted(Comparator.comparing(BriefingCandidate::score).reversed())
                .toList();
    }

    private BriefingCandidate higherScore(BriefingCandidate left, BriefingCandidate right) {
        return left.score().compareTo(right.score()) >= 0 ? left : right;
    }

    private String dedupeKey(BriefingCandidate candidate) {
        if (candidate.url() != null && !candidate.url().isBlank()) {
            return "url:" + normalize(candidate.url());
        }
        return "external:" + candidate.source() + ":" + normalize(candidate.externalId());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
