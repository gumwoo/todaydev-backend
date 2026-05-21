package com.todaydev.auth.security;

import com.todaydev.common.exception.ErrorCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityExceptionHandlers {

    private final SecurityErrorResponseWriter responseWriter;

    public SecurityExceptionHandlers(SecurityErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    public Mono<Void> handleAuthentication(ServerWebExchange exchange, AuthenticationException exception) {
        return responseWriter.write(exchange, ErrorCode.AUTH_TOKEN_MISSING);
    }

    public Mono<Void> handleAccessDenied(ServerWebExchange exchange, AccessDeniedException exception) {
        return responseWriter.write(exchange, ErrorCode.AUTH_FORBIDDEN);
    }
}
