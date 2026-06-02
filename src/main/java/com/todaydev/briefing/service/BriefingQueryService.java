package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.Source;
import com.todaydev.briefing.repository.BriefingItemDetail;
import com.todaydev.briefing.repository.BriefingListItem;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.briefing.web.BriefingDetailResponse;
import com.todaydev.briefing.web.BriefingItemResponse;
import com.todaydev.briefing.web.BriefingListItemResponse;
import com.todaydev.briefing.web.BriefingSectionResponse;
import com.todaydev.briefing.web.BriefingsResponse;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BriefingQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BriefingRepository briefingRepository;

    public BriefingQueryService(BriefingRepository briefingRepository) {
        this.briefingRepository = briefingRepository;
    }

    public Mono<BriefingsResponse> findMyBriefings(Long userId, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        Mono<List<BriefingListItemResponse>> itemsMono = briefingRepository
                .findByUserId(userId, normalizedPage, normalizedSize)
                .map(this::toListItemResponse)
                .collectList();
        Mono<Long> totalMono = briefingRepository.countByUserId(userId);

        return Mono.zip(itemsMono, totalMono)
                .map(tuple -> toPageResponse(tuple.getT1(), normalizedPage, normalizedSize, tuple.getT2()));
    }

    public Mono<BriefingDetailResponse> findDetail(Long userId, Long briefingId) {
        Mono<Briefing> briefingMono = briefingRepository.findByIdAndUserId(briefingId, userId)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.BRIEFING_NOT_FOUND)));
        Mono<List<BriefingItemDetail>> itemsMono = briefingRepository.findItemsByBriefingIdAndUserId(briefingId, userId)
                .collectList();

        return Mono.zip(briefingMono, itemsMono)
                .map(tuple -> toResponse(tuple.getT1(), tuple.getT2()));
    }

    private BriefingDetailResponse toResponse(Briefing briefing, List<BriefingItemDetail> items) {
        Map<Source, List<BriefingItemResponse>> grouped = new LinkedHashMap<>();

        for (BriefingItemDetail item : items) {
            grouped.computeIfAbsent(item.source(), ignored -> new ArrayList<>())
                    .add(toItemResponse(item));
        }

        List<BriefingSectionResponse> sections = grouped.entrySet().stream()
                .map(entry -> new BriefingSectionResponse(
                        entry.getKey(),
                        briefing.status(),
                        List.copyOf(entry.getValue())
                ))
                .toList();

        return new BriefingDetailResponse(
                briefing.briefingId(),
                briefing.title(),
                briefing.summary(),
                briefing.status(),
                briefing.generatedAt(),
                sections
        );
    }

    private BriefingItemResponse toItemResponse(BriefingItemDetail item) {
        return new BriefingItemResponse(
                item.itemId(),
                item.source(),
                item.externalId(),
                item.title(),
                item.url(),
                item.summary(),
                item.score(),
                item.publishedAt(),
                item.metadata(),
                item.saved()
        );
    }

    private BriefingsResponse toPageResponse(
            List<BriefingListItemResponse> items,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new BriefingsResponse(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages
        );
    }

    private BriefingListItemResponse toListItemResponse(BriefingListItem item) {
        return new BriefingListItemResponse(
                item.briefingId(),
                item.title(),
                item.summary(),
                item.status(),
                item.generatedAt(),
                item.itemCount()
        );
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }
}
