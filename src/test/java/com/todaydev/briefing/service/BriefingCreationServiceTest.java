package com.todaydev.briefing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.infrastructure.BriefingJobDispatcher;
import com.todaydev.briefing.progress.BriefingProgressService;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BriefingCreationServiceTest {

    @Test
    void create_returnsGeneratingBriefingAndDispatchesBackgroundJob() {
        BriefingLockService lockService = mock(BriefingLockService.class);
        BriefingRepository briefingRepository = mock(BriefingRepository.class);
        BriefingProgressService progressService = mock(BriefingProgressService.class);
        BriefingJobDispatcher dispatcher = mock(BriefingJobDispatcher.class);
        BriefingLockService.BriefingLock lock = new BriefingLockService.BriefingLock("lock");
        Briefing generating = new Briefing(100L, 1L, null, null, BriefingStatus.GENERATING, LocalDateTime.now());
        BriefingCreationService service = new BriefingCreationService(
                lockService, briefingRepository, progressService, dispatcher);

        when(lockService.acquire(1L)).thenReturn(Mono.just(lock));
        when(briefingRepository.createGenerating(1L)).thenReturn(Mono.just(generating));
        when(progressService.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(service.create(1L))
                .assertNext(briefing -> assertThat(briefing.status()).isEqualTo(BriefingStatus.GENERATING))
                .verifyComplete();

        verify(dispatcher).dispatch(new BriefingGenerationJob(1L, 100L, lock));
    }

    @Test
    void create_returnsConflict_whenLockAlreadyExists() {
        BriefingLockService lockService = mock(BriefingLockService.class);
        BriefingCreationService service = new BriefingCreationService(
                lockService, mock(BriefingRepository.class), mock(BriefingProgressService.class),
                mock(BriefingJobDispatcher.class));
        when(lockService.acquire(1L))
                .thenReturn(Mono.error(new TodaydevException(ErrorCode.BRIEFING_ALREADY_IN_PROGRESS)));

        StepVerifier.create(service.create(1L))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode())
                            .isEqualTo(ErrorCode.BRIEFING_ALREADY_IN_PROGRESS);
                })
                .verify();
    }
}
