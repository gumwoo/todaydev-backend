package com.todaydev.auth.web;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.auth.security.AuthCookies;
import com.todaydev.auth.service.AuthResult;
import com.todaydev.auth.service.AuthService;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<SignupResponse>>> signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request)
                .map(response -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response)));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request)
                .map(this::loginResponse);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<RefreshResponse>>> refresh(
            @CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken
    ) {
        return authService.refresh(refreshToken)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<LogoutResponse>>> logout() {
        return currentUser()
                .flatMap(user -> authService.logout(user.userId()))
                .map(response -> ResponseEntity.ok()
                        .header("Set-Cookie", expiredRefreshCookie().toString())
                        .body(ApiResponse.success(response)));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> loginResponse(AuthResult result) {
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie(result).toString())
                .body(ApiResponse.success(result.response()));
    }

    private ResponseCookie refreshCookie(AuthResult result) {
        return ResponseCookie.from(AuthCookies.REFRESH_TOKEN, result.refreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path(AuthCookies.REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(result.refreshTokenMaxAge()))
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(AuthCookies.REFRESH_TOKEN, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path(AuthCookies.REFRESH_TOKEN_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    private Mono<AuthenticatedUser> currentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(context -> context.getAuthentication().getPrincipal())
                .cast(AuthenticatedUser.class)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_TOKEN_MISSING)));
    }
}
