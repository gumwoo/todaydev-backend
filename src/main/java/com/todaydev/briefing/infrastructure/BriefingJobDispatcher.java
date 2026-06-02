package com.todaydev.briefing.infrastructure;

import com.todaydev.briefing.service.BriefingGenerationJob;
import com.todaydev.briefing.service.BriefingGenerationService;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

@Component
public class BriefingJobDispatcher {

    private static final Logger log = LoggerFactory.getLogger(BriefingJobDispatcher.class);

    private final BriefingGenerationService generationService;
    private final Sinks.Many<BriefingGenerationJob> jobs = Sinks.many().unicast().onBackpressureBuffer();
    private Disposable worker;

    public BriefingJobDispatcher(BriefingGenerationService generationService) {
        this.generationService = generationService;
    }

    @PostConstruct
    void start() {
        // This is the single infrastructure subscription boundary for background briefing jobs.
        worker = jobs.asFlux()
                .flatMap(job -> generationService.process(job)
                        .onErrorResume(error -> {
                            log.error("Briefing background job failed: briefingId={}", job.briefingId(), error);
                            return reactor.core.publisher.Mono.empty();
                        }), 4)
                .subscribe(
                        ignored -> { },
                        error -> log.error("Briefing background worker stopped unexpectedly", error)
                );
    }

    public void dispatch(BriefingGenerationJob job) {
        Sinks.EmitResult result = jobs.tryEmitNext(job);
        if (result.isFailure()) {
            throw new TodaydevException(ErrorCode.BRIEFING_CREATE_FAILED);
        }
    }

    @PreDestroy
    void stop() {
        if (worker != null) {
            worker.dispose();
        }
    }
}
