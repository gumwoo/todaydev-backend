package com.todaydev.auth.web;

public record SignupResponse(
        Long userId,
        String email
) {
}
