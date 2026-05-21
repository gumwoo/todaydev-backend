package com.todaydev.auth.web;

public record UserResponse(
        Long userId,
        String email
) {
}
