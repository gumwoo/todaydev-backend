package com.todaydev.auth.filter;

import com.todaydev.auth.domain.AuthenticatedUser;
import com.todaydev.auth.security.JwtClaims;
import com.todaydev.auth.security.JwtProvider;
import com.todaydev.auth.security.SecurityErrorResponseWriter;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh"
    );

    private final JwtProvider jwtProvider;
    private final SecurityErrorResponseWriter responseWriter;

    public JwtAuthenticationWebFilter(
            JwtProvider jwtProvider,
            SecurityErrorResponseWriter responseWriter
    ) {
        this.jwtProvider = jwtProvider;
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (isPublicPath(exchange.getRequest())) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        return Mono.fromCallable(() -> authenticate(authorization.substring(BEARER_PREFIX.length())))
                .flatMap(authentication -> chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)))
                .onErrorResume(TodaydevException.class, exception -> responseWriter.write(exchange, exception.errorCode()));
    }

    private Authentication authenticate(String accessToken) {
        JwtClaims claims = jwtProvider.parseAccessToken(accessToken);
        AuthenticatedUser principal = new AuthenticatedUser(claims.userId(), claims.email());
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private boolean isPublicPath(ServerHttpRequest request) {
        String path = request.getPath().pathWithinApplication().value();
        return PUBLIC_PATHS.contains(path);
    }
}
