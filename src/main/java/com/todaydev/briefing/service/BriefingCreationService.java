package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.infrastructure.BriefingJobDispatcher;
import com.todaydev.briefing.progress.BriefingProgressPayload;
import com.todaydev.briefing.progress.BriefingProgressService;
import com.todaydev.briefing.progress.BriefingStreamMessage;
import com.todaydev.briefing.progress.ProgressStep;
import com.todaydev.briefing.repository.BriefingRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BriefingCreationService {

    private final BriefingLockService lockService;
    private final BriefingRepository briefingRepository;
    private final BriefingProgressService progressService;
    private final BriefingJobDispatcher jobDispatcher;

    public BriefingCreationService(
            BriefingLockService lockService,
            BriefingRepository briefingRepository,
            BriefingProgressService progressService,
            BriefingJobDispatcher jobDispatcher
    ) {
        this.lockService = lockService;
        this.briefingRepository = briefingRepository;
        this.progressService = progressService;
        this.jobDispatcher = jobDispatcher;
    }

    public Mono<Briefing> create(Long userId) {
        return lockService.acquire(userId)
                .flatMap(lock -> briefingRepository.createGenerating(userId)
                        .flatMap(briefing -> progressService.publish(new BriefingStreamMessage(
                                        BriefingStreamMessage.PROGRESS_EVENT,
                                        BriefingProgressPayload.progress(
                                                briefing.briefingId(),
                                                ProgressStep.BRIEFING_REQUESTED,
                                                null,
                                                null,
                                                null,
                                                "Briefing request accepted"
                                        )))
                                .then(Mono.fromRunnable(() -> jobDispatcher.dispatch(
                                        new BriefingGenerationJob(userId, briefing.briefingId(), lock))))
                                .thenReturn(briefing))
                        .onErrorResume(error -> lockService.release(lock).then(Mono.error(error))));
    }
}
