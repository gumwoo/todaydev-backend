package com.todaydev.savedarticle.web;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.common.response.ApiResponse;
import com.todaydev.preference.web.DeleteResponse;
import com.todaydev.savedarticle.service.SavedArticleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/saved-articles")
public class SavedArticleController {

    private final SavedArticleService savedArticleService;

    public SavedArticleController(SavedArticleService savedArticleService) {
        this.savedArticleService = savedArticleService;
    }

    @PostMapping("/{itemId}")
    public Mono<ResponseEntity<ApiResponse<SavedArticleResponse>>> save(
            @PathVariable Long itemId,
            @Valid @RequestBody SaveArticleRequest request
    ) {
        return currentUser()
                .flatMap(user -> savedArticleService.save(user.userId(), itemId, request))
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response)));
    }

    @GetMapping
    public Mono<ApiResponse<SavedArticlesResponse>> findMySavedArticles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return currentUser()
                .flatMap(user -> savedArticleService.findMySavedArticles(user.userId(), page, size))
                .map(ApiResponse::success);
    }

    @PatchMapping("/{savedId}")
    public Mono<ApiResponse<SavedArticleResponse>> updateMemo(
            @PathVariable Long savedId,
            @Valid @RequestBody UpdateSavedArticleRequest request
    ) {
        return currentUser()
                .flatMap(user -> savedArticleService.updateMemo(user.userId(), savedId, request))
                .map(ApiResponse::success);
    }

    @DeleteMapping("/{savedId}")
    public Mono<ApiResponse<DeleteResponse>> delete(@PathVariable Long savedId) {
        return currentUser()
                .flatMap(user -> savedArticleService.delete(user.userId(), savedId))
                .map(ApiResponse::success);
    }

    private Mono<AuthenticatedUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_TOKEN_MISSING)));
    }
}
