package com.todaydev.ai;

import com.todaydev.common.config.properties.ExternalApiProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class GeminiClient {

    private final WebClient webClient;
    private final ExternalApiProperties properties;

    public GeminiClient(
            @Qualifier("geminiWebClient") WebClient webClient,
            ExternalApiProperties properties
    ) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public Mono<String> generateSummary(String prompt) {
        if (properties.gemini().apiKey() == null || properties.gemini().apiKey().isBlank()) {
            return Mono.error(new TodaydevException(ErrorCode.AI_SUMMARY_FAILED));
        }

        return webClient.post()
                .uri("/v1beta/models/{model}:generateContent", properties.gemini().model())
                .bodyValue(GeminiGenerateContentRequest.text(prompt))
                .retrieve()
                .onStatus(status -> status.value() == 429,
                        response -> Mono.error(new TodaydevException(ErrorCode.AI_RATE_LIMITED)))
                .onStatus(status -> status.isError(),
                        response -> Mono.error(new TodaydevException(ErrorCode.AI_SUMMARY_FAILED)))
                .bodyToMono(GeminiGenerateContentResponse.class)
                .map(GeminiGenerateContentResponse::firstText)
                .filter(text -> !text.isBlank())
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AI_SUMMARY_FAILED)))
                .timeout(properties.client().timeout())
                .retryWhen(Retry.backoff(Math.max(0, properties.client().retryMaxAttempts() - 1),
                                properties.client().retryBackoff())
                        .filter(this::isRetryable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()))
                .onErrorMap(this::mapUnexpected);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof TodaydevException exception) {
            ErrorCode errorCode = exception.errorCode();
            return errorCode == ErrorCode.AI_SUMMARY_FAILED || errorCode == ErrorCode.AI_TIMEOUT;
        }
        return throwable instanceof TimeoutException || throwable instanceof WebClientRequestException;
    }

    private Throwable mapUnexpected(Throwable throwable) {
        if (throwable instanceof TodaydevException) {
            return throwable;
        }
        if (throwable instanceof TimeoutException) {
            return new TodaydevException(ErrorCode.AI_TIMEOUT);
        }
        return new TodaydevException(ErrorCode.AI_SUMMARY_FAILED);
    }
}
