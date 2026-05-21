package com.todaydev.auth.service;

import com.todaydev.auth.web.LoginResponse;

public record AuthResult(
        LoginResponse response,
        String refreshToken,
        long refreshTokenMaxAge
) {
}
