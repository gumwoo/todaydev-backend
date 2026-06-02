package com.todaydev.preference.web;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.common.response.ApiResponse;
import com.todaydev.preference.service.PreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/preferences/me")
public class PreferenceController {

    private final PreferenceService preferenceService;

    public PreferenceController(PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public Mono<ApiResponse<PreferenceResponse>> getMyPreferences() {
        return currentUser()
                .flatMap(user -> preferenceService.getMyPreferences(user.userId()))
                .map(ApiResponse::success);
    }

    @PostMapping("/keywords")
    public Mono<ResponseEntity<ApiResponse<KeywordResponse>>> createKeyword(
            @Valid @RequestBody CreateKeywordRequest request
    ) {
        return currentUser()
                .flatMap(user -> preferenceService.createKeyword(user.userId(), request))
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response)));
    }

    @DeleteMapping("/keywords/{keywordId}")
    public Mono<ApiResponse<DeleteResponse>> deleteKeyword(@PathVariable Long keywordId) {
        return currentUser()
                .flatMap(user -> preferenceService.deleteKeyword(user.userId(), keywordId))
                .map(ApiResponse::success);
    }

    @PostMapping("/repositories")
    public Mono<ResponseEntity<ApiResponse<RepositoryResponse>>> createRepository(
            @Valid @RequestBody CreateRepositoryRequest request
    ) {
        return currentUser()
                .flatMap(user -> preferenceService.createRepository(user.userId(), request))
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response)));
    }

    @DeleteMapping("/repositories/{repositoryId}")
    public Mono<ApiResponse<DeleteResponse>> deleteRepository(@PathVariable Long repositoryId) {
        return currentUser()
                .flatMap(user -> preferenceService.deleteRepository(user.userId(), repositoryId))
                .map(ApiResponse::success);
    }

    private Mono<AuthenticatedUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_TOKEN_MISSING)));
    }
}
