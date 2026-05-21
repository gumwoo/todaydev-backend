package com.todaydev.common.exception;

import com.todaydev.common.response.ApiError;
import com.todaydev.common.response.ApiErrorDetail;
import com.todaydev.common.response.ApiResponse;
import com.todaydev.common.trace.TraceIds;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TodaydevException.class)
    public ResponseEntity<ApiResponse<Void>> handleTodaydevException(
            TodaydevException exception,
            ServerWebExchange exchange
    ) {
        ErrorCode errorCode = exception.errorCode();
        return error(errorCode, exception.details(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiResponse<Void>> handleWebExchangeBindException(
            WebExchangeBindException exception,
            ServerWebExchange exchange
    ) {
        List<ApiErrorDetail> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> ApiErrorDetail.of(error.getField(), safeReason(error.getDefaultMessage())))
                .toList();

        return error(ErrorCode.VALIDATION_FAILED, details, exchange);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception,
            ServerWebExchange exchange
    ) {
        List<ApiErrorDetail> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> ApiErrorDetail.of(
                        violation.getPropertyPath().toString(),
                        safeReason(violation.getMessage())
                ))
                .sorted(Comparator.comparing(ApiErrorDetail::field, Comparator.nullsLast(String::compareTo)))
                .toList();

        return error(ErrorCode.VALIDATION_FAILED, details, exchange);
    }

    @ExceptionHandler({ServerWebInputException.class, DecodingException.class})
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(
            Exception exception,
            ServerWebExchange exchange
    ) {
        log.debug("Invalid request. traceId={}", TraceIds.from(exchange));
        return error(ErrorCode.INVALID_REQUEST, exchange);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException exception,
            ServerWebExchange exchange
    ) {
        return error(ErrorCode.AUTH_TOKEN_INVALID, exchange);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException exception,
            ServerWebExchange exchange
    ) {
        return error(ErrorCode.AUTH_FORBIDDEN, exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException exception,
            ServerWebExchange exchange
    ) {
        ErrorCode errorCode = switch (exception.getStatusCode().value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 401 -> ErrorCode.AUTH_TOKEN_INVALID;
            case 403 -> ErrorCode.AUTH_FORBIDDEN;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            default -> ErrorCode.INTERNAL_SERVER_ERROR;
        };

        return error(errorCode, exchange);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiResponse<Void>> handleThrowable(
            Throwable exception,
            ServerWebExchange exchange
    ) {
        String traceId = TraceIds.from(exchange);
        log.error("Unhandled exception. traceId={}", traceId, exception);
        return error(ErrorCode.INTERNAL_SERVER_ERROR, List.of(), traceId);
    }

    private ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, ServerWebExchange exchange) {
        return error(errorCode, List.of(), exchange);
    }

    private ResponseEntity<ApiResponse<Void>> error(
            ErrorCode errorCode,
            List<ApiErrorDetail> details,
            ServerWebExchange exchange
    ) {
        return error(errorCode, details, TraceIds.from(exchange));
    }

    private ResponseEntity<ApiResponse<Void>> error(
            ErrorCode errorCode,
            List<ApiErrorDetail> details,
            String traceId
    ) {
        ApiError apiError = new ApiError(
                errorCode.name(),
                errorCode.message(),
                details,
                traceId
        );

        return ResponseEntity
                .status(resolveStatus(errorCode))
                .header(TraceIds.TRACE_ID_HEADER, traceId)
                .body(ApiResponse.failure(apiError));
    }

    private HttpStatus resolveStatus(ErrorCode errorCode) {
        return errorCode.httpStatus();
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "요청 값이 올바르지 않습니다." : reason;
    }
}
