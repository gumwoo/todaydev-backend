package com.todaydev.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.Source;
import com.todaydev.common.config.properties.BriefingProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AiSummaryServiceTest {

    @Test
    void summarize_returnsAiSummary_whenGeminiSucceeds() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        when(geminiClient.generateSummary(any())).thenReturn(Mono.just("AI summary"));

        AiSummaryService service = new AiSummaryService(geminiClient, properties());

        StepVerifier.create(service.summarize(1L, List.of(candidate("A", "10"))))
                .assertNext(result -> {
                    assertThat(result.success()).isTrue();
                    assertThat(result.summary()).isEqualTo("AI summary");
                })
                .verifyComplete();
    }

    @Test
    void summarize_returnsFallbackSummary_whenGeminiFails() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        when(geminiClient.generateSummary(any())).thenReturn(Mono.error(new RuntimeException("boom")));

        AiSummaryService service = new AiSummaryService(geminiClient, properties());

        StepVerifier.create(service.summarize(1L, List.of(candidate("Spring", "10"))))
                .assertNext(result -> {
                    assertThat(result.success()).isFalse();
                    assertThat(result.summary()).contains("1개");
                    assertThat(result.summary()).contains("Spring");
                })
                .verifyComplete();
    }

    private BriefingProperties properties() {
        return new BriefingProperties(
                new BriefingProperties.Scoring(10, 30, 20, 15, 12, 48),
                new BriefingProperties.Collection(5, 3, 20, 5, 8),
                new BriefingProperties.Ai(10)
        );
    }

    private BriefingCandidate candidate(String title, String score) {
        return new BriefingCandidate(
                Source.DEVTO,
                title,
                title,
                "https://example.com/" + title,
                null,
                OffsetDateTime.now(),
                Map.of(),
                List.of(),
                false,
                new BigDecimal(score)
        );
    }
}
