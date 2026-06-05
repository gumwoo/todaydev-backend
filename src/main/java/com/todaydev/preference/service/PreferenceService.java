package com.todaydev.preference.service;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import com.todaydev.preference.repository.PreferenceRepository;
import com.todaydev.preference.web.CreateKeywordRequest;
import com.todaydev.preference.web.CreateRepositoryRequest;
import com.todaydev.preference.web.DeleteResponse;
import com.todaydev.preference.web.KeywordResponse;
import com.todaydev.preference.web.PreferenceResponse;
import com.todaydev.preference.web.RepositoryResponse;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    public PreferenceService(PreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public Mono<PreferenceResponse> getMyPreferences(Long userId) {
        Mono<java.util.List<KeywordResponse>> keywords = preferenceRepository.findKeywordsByUserId(userId)
                .map(this::toKeywordResponse)
                .collectList();
        Mono<java.util.List<RepositoryResponse>> repositories = preferenceRepository.findRepositoriesByUserId(userId)
                .map(this::toRepositoryResponse)
                .collectList();

        return Mono.zip(keywords, repositories)
                .map(tuple -> new PreferenceResponse(tuple.getT1(), tuple.getT2()));
    }

    public Mono<KeywordResponse> createKeyword(Long userId, CreateKeywordRequest request) {
        String keyword = normalizeKeyword(request.keyword());
        int weight = request.weight() == null ? 1 : request.weight();

        return preferenceRepository.findKeywordsByUserId(userId)
                .any(savedKeyword -> sameKeyword(savedKeyword.keyword(), keyword))
                .flatMap(duplicated -> duplicated
                        ? Mono.error(new TodaydevException(ErrorCode.PREFERENCE_KEYWORD_DUPLICATED))
                        : preferenceRepository.saveKeyword(userId, keyword, weight))
                .map(this::toKeywordResponse)
                .onErrorMap(DuplicateKeyException.class,
                        exception -> new TodaydevException(ErrorCode.PREFERENCE_KEYWORD_DUPLICATED));
    }

    public Mono<DeleteResponse> deleteKeyword(Long userId, Long keywordId) {
        return preferenceRepository.deleteKeyword(userId, keywordId)
                .flatMap(deleted -> deleted
                        ? Mono.just(new DeleteResponse(true))
                        : Mono.error(new TodaydevException(ErrorCode.PREFERENCE_KEYWORD_NOT_FOUND)));
    }

    public Mono<RepositoryResponse> createRepository(Long userId, CreateRepositoryRequest request) {
        String owner = RepositoryNameValidator.normalizeOwner(request.owner());
        String repoName = RepositoryNameValidator.normalizeRepoName(request.repoName());

        return preferenceRepository.saveRepository(userId, owner, repoName)
                .map(this::toRepositoryResponse)
                .onErrorMap(DuplicateKeyException.class,
                        exception -> new TodaydevException(ErrorCode.PREFERENCE_REPOSITORY_DUPLICATED));
    }

    public Mono<DeleteResponse> deleteRepository(Long userId, Long repositoryId) {
        return preferenceRepository.deleteRepository(userId, repositoryId)
                .flatMap(deleted -> deleted
                        ? Mono.just(new DeleteResponse(true))
                        : Mono.error(new TodaydevException(ErrorCode.PREFERENCE_REPOSITORY_NOT_FOUND)));
    }

    private KeywordResponse toKeywordResponse(InterestKeyword keyword) {
        return new KeywordResponse(
                keyword.keywordId(),
                keyword.keyword(),
                keyword.weight(),
                keyword.createdAt()
        );
    }

    private RepositoryResponse toRepositoryResponse(WatchedRepository repository) {
        return new RepositoryResponse(
                repository.repositoryId(),
                repository.owner(),
                repository.repoName(),
                repository.createdAt()
        );
    }

    private String normalizeKeyword(String keyword) {
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private boolean sameKeyword(String left, String right) {
        return normalizeKeyword(left).equals(normalizeKeyword(right));
    }
}
