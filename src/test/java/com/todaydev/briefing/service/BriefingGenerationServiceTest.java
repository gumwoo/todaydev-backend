package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todaydev.ai.AiSummaryResult;
import com.todaydev.ai.AiSummaryService;
import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingItem;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.domain.Source;
import com.todaydev.briefing.progress.BriefingProgressService;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.common.config.properties.BriefingProperties;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import com.todaydev.preference.repository.PreferenceRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BriefingGenerationServiceTest {

    @Test
    void process_completesAndReleasesLock_whenSourcesAndAiSucceed() {
        TestContext context = new TestContext();
        context.sourceResults(List.of(
                success(Source.GITHUB, article(ExternalSource.GITHUB, "1", "https://example.com/github")),
                success(Source.HACKER_NEWS, article(ExternalSource.HACKER_NEWS, "2", "https://example.com/hn")),
                success(Source.DEVTO, article(ExternalSource.DEVTO, "3", "https://example.com/devto"))
        ));
        context.aiResult(true);
        context.updatedStatus(BriefingStatus.COMPLETED);

        StepVerifier.create(context.service.process(context.job)).verifyComplete();

        verify(context.briefingRepository).updateStatus(eq(100L), eq(BriefingStatus.COMPLETED), any(), any());
        verify(context.lockService).release(context.job.lock());
    }

    @Test
    void process_persistsSummaryFailed_whenAiFallsBack() {
        TestContext context = new TestContext();
        context.sourceResults(List.of(
                success(Source.GITHUB, article(ExternalSource.GITHUB, "1", "https://example.com/github")),
                success(Source.HACKER_NEWS, article(ExternalSource.HACKER_NEWS, "2", "https://example.com/hn")),
                success(Source.DEVTO, article(ExternalSource.DEVTO, "3", "https://example.com/devto"))
        ));
        context.aiResult(false);
        context.updatedStatus(BriefingStatus.SUMMARY_FAILED);

        StepVerifier.create(context.service.process(context.job)).verifyComplete();

        verify(context.briefingRepository).updateStatus(eq(100L), eq(BriefingStatus.SUMMARY_FAILED), any(), any());
    }

    private static SourceCollectionResult success(Source source, ExternalArticle article) {
        return new SourceCollectionResult(source, List.of(article), ApiCallLog.success(100L, source, 10));
    }

    private static ExternalArticle article(ExternalSource source, String externalId, String url) {
        return new ExternalArticle(
                source,
                externalId,
                "Spring WebFlux",
                url,
                "Reactive article",
                OffsetDateTime.now(),
                Map.of("owner", "spring-projects", "repoName", "spring-framework")
        );
    }

    private static class TestContext {

        private final BriefingLockService lockService = mock(BriefingLockService.class);
        private final PreferenceRepository preferenceRepository = mock(PreferenceRepository.class);
        private final BriefingRepository briefingRepository = mock(BriefingRepository.class);
        private final ExternalArticleCollector articleCollector = mock(ExternalArticleCollector.class);
        private final AiSummaryService aiSummaryService = mock(AiSummaryService.class);
        private final BriefingProgressService progressService = mock(BriefingProgressService.class);
        private final BriefingGenerationJob job = new BriefingGenerationJob(
                1L, 100L, new BriefingLockService.BriefingLock("lock"));
        private final BriefingGenerationService service;

        TestContext() {
            BriefingProperties properties = new BriefingProperties(
                    new BriefingProperties.Scoring(10, 30, 20, 15, 12, 48),
                    new BriefingProperties.Collection(5, 20, 5, 8),
                    new BriefingProperties.Ai(10)
            );

            when(lockService.release(any())).thenReturn(Mono.empty());
            when(progressService.publish(any())).thenReturn(Mono.empty());
            when(preferenceRepository.findKeywordsByUserId(1L)).thenReturn(Flux.just(
                    new InterestKeyword(1L, 1L, "webflux", 5, null)
            ));
            when(preferenceRepository.findRepositoriesByUserId(1L)).thenReturn(Flux.just(
                    new WatchedRepository(1L, 1L, "spring-projects", "spring-framework", null)
            ));
            when(briefingRepository.saveApiCallLogs(any())).thenReturn(Mono.empty());
            when(briefingRepository.saveItems(eq(100L), any())).thenReturn(Flux.just(
                    new BriefingItem(1L, 100L, Source.GITHUB, "1", "Spring", "https://example.com", "",
                            BigDecimal.TEN, LocalDateTime.now(), Map.of("owner", "spring-projects"))
            ));

            service = new BriefingGenerationService(
                    lockService,
                    preferenceRepository,
                    briefingRepository,
                    articleCollector,
                    aiSummaryService,
                    new BriefingCandidateFactory(
                            new KeywordMatcher(),
                            new WatchedRepositoryMatcher(),
                            new BriefingScorer(properties)
                    ),
                    new BriefingDeduplicator(),
                    progressService,
                    properties
            );
        }

        void sourceResults(List<SourceCollectionResult> results) {
            when(articleCollector.collect(eq(100L), any(), any())).thenReturn(Mono.just(results));
        }

        void aiResult(boolean success) {
            when(aiSummaryService.summarize(eq(100L), any())).thenReturn(Mono.just(
                    new AiSummaryResult("AI summary", success, success
                            ? ApiCallLog.success(100L, Source.AI, 10)
                            : ApiCallLog.failed(100L, Source.AI, 10))
            ));
        }

        void updatedStatus(BriefingStatus status) {
            when(briefingRepository.updateStatus(eq(100L), eq(status), any(), any())).thenReturn(Mono.just(
                    new Briefing(100L, 1L, "Todaydev briefing", "AI summary", status, LocalDateTime.now())
            ));
        }
    }
}
