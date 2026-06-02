package com.todaydev.briefing.web;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.progress.BriefingProgressPayload;
import com.todaydev.briefing.progress.BriefingProgressService;
import com.todaydev.briefing.progress.StreamTokenResponse;
import com.todaydev.briefing.progress.StreamTokenService;
import com.todaydev.briefing.service.BriefingCreationService;
import com.todaydev.briefing.service.BriefingQueryService;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/briefings")
public class BriefingController {

    private final BriefingCreationService briefingCreationService;
    private final BriefingQueryService briefingQueryService;
    private final StreamTokenService streamTokenService;
    private final BriefingProgressService progressService;

    public BriefingController(
            BriefingCreationService briefingCreationService,
            BriefingQueryService briefingQueryService,
            StreamTokenService streamTokenService,
            BriefingProgressService progressService
    ) {
        this.briefingCreationService = briefingCreationService;
        this.briefingQueryService = briefingQueryService;
        this.streamTokenService = streamTokenService;
        this.progressService = progressService;
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<CreateBriefingResponse>>> createBriefing() {
        return currentUser()
                .flatMap(user -> briefingCreationService.create(user.userId()))
                .map(briefing -> ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(ApiResponse.success(toResponse(briefing))));
    }

    @GetMapping
    public Mono<ApiResponse<BriefingsResponse>> findBriefings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return currentUser()
                .flatMap(user -> briefingQueryService.findMyBriefings(user.userId(), page, size))
                .map(ApiResponse::success);
    }

    @GetMapping("/{briefingId}")
    public Mono<ApiResponse<BriefingDetailResponse>> findBriefing(@PathVariable Long briefingId) {
        return currentUser()
                .flatMap(user -> briefingQueryService.findDetail(user.userId(), briefingId))
                .map(ApiResponse::success);
    }

    @PostMapping("/{briefingId}/stream-token")
    public Mono<ApiResponse<StreamTokenResponse>> issueStreamToken(@PathVariable Long briefingId) {
        return currentUser()
                .flatMap(user -> streamTokenService.issue(briefingId, user.userId()))
                .map(ApiResponse::success);
    }

    @GetMapping(value = "/{briefingId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<BriefingProgressPayload>> stream(
            @PathVariable Long briefingId,
            @RequestParam String streamToken
    ) {
        return streamTokenService.consume(briefingId, streamToken)
                .thenMany(progressService.stream(briefingId))
                .map(message -> ServerSentEvent.<BriefingProgressPayload>builder(message.payload())
                        .event(message.eventName())
                        .build());
    }

    private CreateBriefingResponse toResponse(Briefing briefing) {
        return new CreateBriefingResponse(
                briefing.briefingId(),
                briefing.status(),
                briefing.generatedAt()
        );
    }

    private Mono<AuthenticatedUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_TOKEN_MISSING)));
    }
}
