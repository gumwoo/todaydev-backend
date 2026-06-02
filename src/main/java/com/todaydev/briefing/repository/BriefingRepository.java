package com.todaydev.briefing.repository;

import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingItem;
import com.todaydev.briefing.domain.BriefingStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BriefingRepository {

    Mono<Briefing> createGenerating(Long userId);

    Mono<Briefing> findByIdAndUserId(Long briefingId, Long userId);

    Flux<BriefingListItem> findByUserId(Long userId, int page, int size);

    Mono<Long> countByUserId(Long userId);

    Flux<BriefingItemDetail> findItemsByBriefingIdAndUserId(Long briefingId, Long userId);

    Mono<Briefing> updateStatus(Long briefingId, BriefingStatus status, String title, String summary);

    Flux<BriefingItem> saveItems(Long briefingId, Iterable<BriefingItem> items);

    Mono<Void> saveApiCallLogs(Iterable<ApiCallLog> logs);
}
