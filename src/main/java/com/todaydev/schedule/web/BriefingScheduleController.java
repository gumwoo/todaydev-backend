package com.todaydev.schedule.web;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.common.response.ApiResponse;
import com.todaydev.schedule.service.BriefingScheduleService;
import jakarta.validation.Valid;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/schedule/me/briefing")
public class BriefingScheduleController {

    private final BriefingScheduleService scheduleService;

    public BriefingScheduleController(BriefingScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public Mono<ApiResponse<BriefingScheduleResponse>> getMySchedule() {
        return currentUser()
                .flatMap(user -> scheduleService.getMySchedule(user.userId()))
                .map(ApiResponse::success);
    }

    @PutMapping
    public Mono<ApiResponse<BriefingScheduleResponse>> updateMySchedule(
            @Valid @RequestBody BriefingScheduleRequest request
    ) {
        return currentUser()
                .flatMap(user -> scheduleService.updateMySchedule(user.userId(), request))
                .map(ApiResponse::success);
    }

    private Mono<AuthenticatedUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_TOKEN_MISSING)));
    }
}
