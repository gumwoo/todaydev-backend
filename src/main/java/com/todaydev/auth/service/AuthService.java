package com.todaydev.auth.service;

import com.todaydev.auth.domain.User;
import com.todaydev.auth.repository.UserRepository;
import com.todaydev.auth.security.JwtClaims;
import com.todaydev.auth.security.JwtProvider;
import com.todaydev.auth.web.LoginRequest;
import com.todaydev.auth.web.LoginResponse;
import com.todaydev.auth.web.LogoutResponse;
import com.todaydev.auth.web.RefreshResponse;
import com.todaydev.auth.web.SignupRequest;
import com.todaydev.auth.web.SignupResponse;
import com.todaydev.auth.web.UserResponse;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.schedule.repository.BriefingScheduleRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final BriefingScheduleRepository scheduleRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtProvider jwtProvider,
            RefreshTokenService refreshTokenService,
            BriefingScheduleRepository scheduleRepository
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtProvider = jwtProvider;
        this.refreshTokenService = refreshTokenService;
        this.scheduleRepository = scheduleRepository;
    }

    public Mono<SignupResponse> signup(SignupRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        return userRepository.existsByEmail(normalizedEmail)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new TodaydevException(ErrorCode.CONFLICT));
                    }
                    return passwordService.encode(request.password())
                            .flatMap(passwordHash -> userRepository.save(normalizedEmail, passwordHash));
                })
                .flatMap(user -> scheduleRepository.createDefault(user.userId()).thenReturn(user))
                .map(user -> new SignupResponse(user.userId(), user.email()))
                .onErrorMap(DuplicateKeyException.class, exception -> new TodaydevException(ErrorCode.CONFLICT));
    }

    public Mono<AuthResult> login(LoginRequest request) {
        String normalizedEmail = normalizeEmail(request.email());
        return userRepository.findByEmail(normalizedEmail)
                .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_INVALID_CREDENTIALS)))
                .flatMap(user -> passwordService.matches(request.password(), user.passwordHash())
                        .flatMap(matches -> matches
                                ? issueTokens(user)
                                : Mono.error(new TodaydevException(ErrorCode.AUTH_INVALID_CREDENTIALS))));
    }

    public Mono<RefreshResponse> refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new TodaydevException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
        }

        return refreshTokenService.verify(refreshToken)
                .flatMap(claims -> userRepository.findById(claims.userId())
                        .switchIfEmpty(Mono.error(new TodaydevException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID))))
                .map(user -> new RefreshResponse(
                        jwtProvider.createAccessToken(user),
                        TOKEN_TYPE,
                        jwtProvider.accessTokenExpiresIn()
                ));
    }

    public Mono<LogoutResponse> logout(Long userId) {
        return refreshTokenService.delete(userId)
                .thenReturn(new LogoutResponse(true));
    }

    private Mono<AuthResult> issueTokens(User user) {
        String accessToken = jwtProvider.createAccessToken(user);
        String refreshToken = jwtProvider.createRefreshToken(user);
        LoginResponse response = new LoginResponse(
                accessToken,
                TOKEN_TYPE,
                jwtProvider.accessTokenExpiresIn(),
                new UserResponse(user.userId(), user.email())
        );

        return refreshTokenService.save(user.userId(), refreshToken)
                .thenReturn(new AuthResult(response, refreshToken, jwtProvider.refreshTokenExpiresIn()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
