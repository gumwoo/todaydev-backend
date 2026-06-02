package com.todaydev.briefing.service;

public record BriefingGenerationJob(
        Long userId,
        Long briefingId,
        BriefingLockService.BriefingLock lock
) {
}
