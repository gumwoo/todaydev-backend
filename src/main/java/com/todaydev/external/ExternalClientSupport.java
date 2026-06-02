package com.todaydev.external;

import com.todaydev.common.config.properties.ExternalApiProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public class ExternalClientSupport {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final ExternalApiProperties.Client policy;

    public ExternalClientSupport(ExternalApiProperties.Client policy) {
        this.policy = policy;
    }

    public int normalizedLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    public Duration timeout() {
        return policy.timeout();
    }

    public Retry retrySpec() {
        return Retry.backoff(Math.max(0, policy.retryMaxAttempts() - 1), policy.retryBackoff())
                .filter(this::isRetryable)
                .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    public Mono<? extends Throwable> mapStatus(ClientResponse response, ErrorCode sourceFailureCode) {
        HttpStatusCode statusCode = response.statusCode();
        if (statusCode.value() == 429) {
            return Mono.error(new TodaydevException(ErrorCode.EXTERNAL_RATE_LIMITED));
        }
        return Mono.error(new TodaydevException(sourceFailureCode));
    }

    public Throwable mapUnexpected(Throwable throwable, ErrorCode sourceFailureCode) {
        if (throwable instanceof TodaydevException) {
            return throwable;
        }
        if (throwable instanceof TimeoutException) {
            return new TodaydevException(ErrorCode.EXTERNAL_TIMEOUT);
        }
        if (throwable instanceof WebClientRequestException) {
            return new TodaydevException(sourceFailureCode);
        }
        return new TodaydevException(sourceFailureCode);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof TodaydevException exception) {
            ErrorCode errorCode = exception.errorCode();
            return errorCode == ErrorCode.EXTERNAL_TIMEOUT
                    || errorCode == ErrorCode.EXTERNAL_GITHUB_FAILED
                    || errorCode == ErrorCode.EXTERNAL_HACKER_NEWS_FAILED
                    || errorCode == ErrorCode.EXTERNAL_DEVTO_FAILED;
        }
        return throwable instanceof TimeoutException || throwable instanceof WebClientRequestException;
    }
}
