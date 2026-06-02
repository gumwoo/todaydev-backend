package com.todaydev.preference.repository;

import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PreferenceRepository {

    Flux<InterestKeyword> findKeywordsByUserId(Long userId);

    Mono<InterestKeyword> saveKeyword(Long userId, String keyword, int weight);

    Mono<Boolean> deleteKeyword(Long userId, Long keywordId);

    Flux<WatchedRepository> findRepositoriesByUserId(Long userId);

    Mono<WatchedRepository> saveRepository(Long userId, String owner, String repoName);

    Mono<Boolean> deleteRepository(Long userId, Long repositoryId);
}
