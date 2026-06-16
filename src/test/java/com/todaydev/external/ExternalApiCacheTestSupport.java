package com.todaydev.external;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import reactor.core.publisher.Flux;

public final class ExternalApiCacheTestSupport {

    private ExternalApiCacheTestSupport() {
    }

    @SuppressWarnings("unchecked")
    public static ExternalApiCache passthroughCache() {
        ExternalApiCache cache = mock(ExternalApiCache.class);
        when(cache.cachedFlux(anyString(), anyString(), any(), any()))
                .thenAnswer(invocation -> ((Supplier<Flux<?>>) invocation.getArgument(3)).get());
        return cache;
    }
}
