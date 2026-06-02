package com.todaydev.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 리소스 상태와 충돌합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "인증 토큰이 올바르지 않습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "인증 토큰이 만료되었습니다."),
    AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh Token이 올바르지 않습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    PREFERENCE_KEYWORD_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 관심 키워드입니다."),
    PREFERENCE_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "관심 키워드를 찾을 수 없습니다."),
    PREFERENCE_REPOSITORY_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 관심 Repository입니다."),
    PREFERENCE_REPOSITORY_NOT_FOUND(HttpStatus.NOT_FOUND, "관심 Repository를 찾을 수 없습니다."),
    PREFERENCE_REPOSITORY_FORMAT_INVALID(HttpStatus.BAD_REQUEST, "Repository 형식이 올바르지 않습니다."),

    BRIEFING_NOT_FOUND(HttpStatus.NOT_FOUND, "브리핑을 찾을 수 없습니다."),
    BRIEFING_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "이미 생성 중인 브리핑이 있습니다."),
    BRIEFING_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "브리핑 생성에 실패했습니다."),
    BRIEFING_PARTIAL_CREATED(HttpStatus.OK, "일부 출처를 제외하고 브리핑을 생성했습니다."),
    BRIEFING_SUMMARY_FAILED(HttpStatus.OK, "AI 요약에 실패했습니다."),

    SAVED_ARTICLE_DUPLICATED(HttpStatus.CONFLICT, "이미 저장한 글입니다."),
    SAVED_ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "저장한 글을 찾을 수 없습니다."),

    EXTERNAL_GITHUB_FAILED(HttpStatus.BAD_GATEWAY, "GitHub API 호출에 실패했습니다."),
    EXTERNAL_HACKER_NEWS_FAILED(HttpStatus.BAD_GATEWAY, "Hacker News API 호출에 실패했습니다."),
    EXTERNAL_DEVTO_FAILED(HttpStatus.BAD_GATEWAY, "DEV.to API 호출에 실패했습니다."),
    EXTERNAL_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "외부 API 호출 한도를 초과했습니다."),
    EXTERNAL_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 응답 시간이 초과되었습니다."),

    AI_SUMMARY_FAILED(HttpStatus.BAD_GATEWAY, "AI 요약 생성에 실패했습니다."),
    AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI API 호출 한도를 초과했습니다."),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI API 응답 시간이 초과되었습니다."),

    STREAM_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "스트림 토큰이 올바르지 않습니다."),
    STREAM_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "스트림 토큰이 만료되었습니다."),
    STREAM_NOT_FOUND(HttpStatus.NOT_FOUND, "스트림을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String message() {
        return message;
    }
}
