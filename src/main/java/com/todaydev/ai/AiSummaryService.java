package com.todaydev.ai;

import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.Source;
import com.todaydev.common.config.properties.BriefingProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AiSummaryService {

    private final GeminiClient geminiClient;
    private final BriefingProperties.Ai aiProperties;

    public AiSummaryService(GeminiClient geminiClient, BriefingProperties properties) {
        this.geminiClient = geminiClient;
        this.aiProperties = properties.ai();
    }

    public Mono<AiSummaryResult> summarize(Long briefingId, List<BriefingCandidate> candidates) {
        Instant startedAt = Instant.now();
        String fallback = fallbackSummary(candidates);

        if (candidates == null || candidates.isEmpty()) {
            return Mono.just(success(briefingId, startedAt, fallback));
        }

        return geminiClient.generateSummary(prompt(candidates))
                .map(summary -> success(briefingId, startedAt, summary))
                .onErrorResume(throwable -> Mono.just(failed(briefingId, startedAt, fallback)));
    }

    private AiSummaryResult success(Long briefingId, Instant startedAt, String summary) {
        return new AiSummaryResult(summary, true, ApiCallLog.success(briefingId, Source.AI, latencyMs(startedAt)));
    }

    private AiSummaryResult failed(Long briefingId, Instant startedAt, String fallback) {
        return new AiSummaryResult(fallback, false, ApiCallLog.failed(briefingId, Source.AI, latencyMs(startedAt)));
    }

    private String prompt(List<BriefingCandidate> candidates) {
        StringBuilder builder = new StringBuilder();
        builder.append("다음 개발 뉴스 후보를 한국어로 3문장 이내로 요약해줘. ");
        builder.append("과장하지 말고 개발자가 오늘 볼 만한 흐름 중심으로 정리해줘.\n\n");

        topCandidates(candidates).forEach(candidate -> builder
                .append("- [")
                .append(candidate.source())
                .append("] ")
                .append(candidate.title())
                .append(" / score=")
                .append(candidate.score())
                .append(" / url=")
                .append(candidate.url())
                .append('\n'));

        return builder.toString();
    }

    private String fallbackSummary(List<BriefingCandidate> candidates) {
        int count = candidates == null ? 0 : candidates.size();
        if (count == 0) {
            return "오늘 수집된 개발 브리핑 후보가 없습니다.";
        }

        String topTitles = String.join(", ", topCandidates(candidates).stream()
                .limit(3)
                .map(BriefingCandidate::title)
                .toList());

        return "오늘 수집된 개발 브리핑 후보는 " + count + "개입니다. 주요 후보: " + topTitles;
    }

    private List<BriefingCandidate> topCandidates(List<BriefingCandidate> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparing(BriefingCandidate::score).reversed())
                .limit(aiProperties.summaryItemLimit())
                .toList();
    }

    private long latencyMs(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
