package com.todaydev.savedarticle.service;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.preference.web.DeleteResponse;
import com.todaydev.savedarticle.domain.SavedArticle;
import com.todaydev.savedarticle.repository.SavedArticleRepository;
import com.todaydev.savedarticle.web.SaveArticleRequest;
import com.todaydev.savedarticle.web.SavedArticleResponse;
import com.todaydev.savedarticle.web.SavedArticlesResponse;
import com.todaydev.savedarticle.web.UpdateSavedArticleRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SavedArticleService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final SavedArticleRepository savedArticleRepository;

    public SavedArticleService(SavedArticleRepository savedArticleRepository) {
        this.savedArticleRepository = savedArticleRepository;
    }

    public Mono<SavedArticleResponse> save(Long userId, Long itemId, SaveArticleRequest request) {
        return savedArticleRepository.save(userId, itemId, normalizeMemo(request.memo()))
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.SAVED_ARTICLE_NOT_FOUND)))
                .map(this::toResponse)
                .onErrorMap(DuplicateKeyException.class,
                        exception -> new TodaydevException(ErrorCode.SAVED_ARTICLE_DUPLICATED));
    }

    public Mono<SavedArticlesResponse> findMySavedArticles(Long userId, Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);

        Mono<java.util.List<SavedArticleResponse>> items = savedArticleRepository
                .findByUserId(userId, normalizedPage, normalizedSize)
                .map(this::toResponse)
                .collectList();
        Mono<Long> total = savedArticleRepository.countByUserId(userId);

        return Mono.zip(items, total)
                .map(tuple -> toPageResponse(tuple.getT1(), normalizedPage, normalizedSize, tuple.getT2()));
    }

    public Mono<SavedArticleResponse> updateMemo(Long userId, Long savedId, UpdateSavedArticleRequest request) {
        return savedArticleRepository.updateMemo(userId, savedId, normalizeMemo(request.memo()))
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.SAVED_ARTICLE_NOT_FOUND)))
                .map(this::toResponse);
    }

    public Mono<DeleteResponse> delete(Long userId, Long savedId) {
        return savedArticleRepository.delete(userId, savedId)
                .flatMap(deleted -> deleted
                        ? Mono.just(new DeleteResponse(true))
                        : Mono.error(new TodaydevException(ErrorCode.SAVED_ARTICLE_NOT_FOUND)));
    }

    private SavedArticlesResponse toPageResponse(
            java.util.List<SavedArticleResponse> items,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new SavedArticlesResponse(
                items,
                page,
                size,
                totalElements,
                totalPages,
                page + 1 < totalPages
        );
    }

    private SavedArticleResponse toResponse(SavedArticle article) {
        return new SavedArticleResponse(
                article.savedId(),
                article.itemId(),
                article.title(),
                article.url(),
                article.source(),
                article.memo(),
                article.savedAt()
        );
    }

    private String normalizeMemo(String memo) {
        if (memo == null) {
            return "";
        }
        return memo.trim();
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
