package com.todaydev.notification.web;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.common.response.ApiResponse;
import com.todaydev.notification.domain.NotificationChannel;
import com.todaydev.notification.service.NotificationDeliveryService;
import com.todaydev.notification.service.NotificationPreferenceService;
import com.todaydev.preference.web.DeleteResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications/me")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;
    private final NotificationDeliveryService deliveryService;

    public NotificationPreferenceController(
            NotificationPreferenceService preferenceService,
            NotificationDeliveryService deliveryService
    ) {
        this.preferenceService = preferenceService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/preferences")
    public Mono<ApiResponse<List<NotificationPreferenceResponse>>> findPreferences() {
        return currentUser()
                .flatMap(user -> preferenceService.findMyPreferences(user.userId()))
                .map(ApiResponse::success);
    }

    @PutMapping("/preferences/{channel}")
    public Mono<ApiResponse<NotificationPreferenceResponse>> updatePreference(
            @PathVariable NotificationChannel channel,
            @Valid @RequestBody NotificationPreferenceRequest request
    ) {
        return currentUser()
                .flatMap(user -> preferenceService.update(user.userId(), channel, request))
                .map(ApiResponse::success);
    }

    @DeleteMapping("/preferences/{channel}")
    public Mono<ApiResponse<DeleteResponse>> deletePreference(@PathVariable NotificationChannel channel) {
        return currentUser()
                .flatMap(user -> preferenceService.delete(user.userId(), channel))
                .map(ApiResponse::success);
    }

    @GetMapping("/deliveries")
    public Mono<ApiResponse<NotificationDeliveriesResponse>> findDeliveries(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return currentUser()
                .flatMap(user -> deliveryService.findMyDeliveries(user.userId(), page, size))
                .map(ApiResponse::success);
    }

    @PostMapping("/test")
    public Mono<ApiResponse<NotificationPreferenceResponse>> sendTest(@Valid @RequestBody TestNotificationRequest request) {
        return currentUser()
                .flatMap(user -> preferenceService.sendTest(user.userId(), request.channel())
                        .then(preferenceService.findMyPreferences(user.userId()))
                        .map(preferences -> preferences.stream()
                                .filter(preference -> preference.channel() == request.channel())
                                .findFirst()
                                .orElseThrow(() -> new TodaydevException(ErrorCode.NOTIFICATION_PREFERENCE_NOT_FOUND))))
                .map(ApiResponse::success);
    }

    private Mono<AuthenticatedUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_TOKEN_MISSING)));
    }
}
