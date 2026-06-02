package com.todaydev.savedarticle.repository;

import com.todaydev.savedarticle.domain.SavedArticle;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SavedArticleRepository {

    Mono<SavedArticle> save(Long userId, Long itemId, String memo);

    Flux<SavedArticle> findByUserId(Long userId, int page, int size);

    Mono<Long> countByUserId(Long userId);

    Mono<SavedArticle> updateMemo(Long userId, Long savedId, String memo);

    Mono<Boolean> delete(Long userId, Long savedId);
}
