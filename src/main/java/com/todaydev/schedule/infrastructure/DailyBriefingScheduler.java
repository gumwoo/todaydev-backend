package com.todaydev.schedule.infrastructure;

import com.todaydev.schedule.service.ScheduledBriefingService;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyBriefingScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyBriefingScheduler.class);

    private final ScheduledBriefingService scheduledBriefingService;

    public DailyBriefingScheduler(ScheduledBriefingService scheduledBriefingService) {
        this.scheduledBriefingService = scheduledBriefingService;
    }

    @Scheduled(
            fixedDelayString = "${briefing.schedule.poll-interval-ms:60000}",
            initialDelayString = "${briefing.schedule.initial-delay-ms:15000}"
    )
    public void enqueueDueBriefings() {
        scheduledBriefingService.enqueueDueBriefings(Instant.now())
                .subscribe(
                        enqueued -> {
                            if (enqueued > 0) {
                                log.info("Scheduled briefing jobs enqueued: count={}", enqueued);
                            }
                        },
                        error -> log.error("Scheduled briefing scan failed", error)
                );
    }
}
