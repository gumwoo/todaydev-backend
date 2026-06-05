package com.todaydev.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.repository.PreferenceRepository;
import com.todaydev.preference.web.CreateKeywordRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PreferenceServiceTest {

    private final PreferenceRepository repository = mock(PreferenceRepository.class);
    private final PreferenceService service = new PreferenceService(repository);

    @Test
    void createKeyword_normalizesKeywordToLowercaseBeforeSaving() {
        when(repository.findKeywordsByUserId(1L)).thenReturn(Flux.empty());
        when(repository.saveKeyword(eq(1L), eq("spring"), eq(5))).thenReturn(Mono.just(
                new InterestKeyword(10L, 1L, "spring", 5, LocalDateTime.now())
        ));

        StepVerifier.create(service.createKeyword(1L, new CreateKeywordRequest(" Spring ", 5)))
                .assertNext(response -> assertThat(response.keyword()).isEqualTo("spring"))
                .verifyComplete();

        verify(repository).saveKeyword(1L, "spring", 5);
    }

    @Test
    void createKeyword_rejectsCaseInsensitiveDuplicate() {
        when(repository.findKeywordsByUserId(1L)).thenReturn(Flux.just(
                new InterestKeyword(10L, 1L, "Spring", 5, LocalDateTime.now())
        ));

        StepVerifier.create(service.createKeyword(1L, new CreateKeywordRequest("spring", 5)))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode())
                            .isEqualTo(ErrorCode.PREFERENCE_KEYWORD_DUPLICATED);
                })
                .verify();

        verify(repository, never()).saveKeyword(eq(1L), eq("spring"), eq(5));
    }
}
