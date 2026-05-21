package com.todaydev.auth.repository;

import com.todaydev.auth.domain.User;
import reactor.core.publisher.Mono;

public interface UserRepository {

    Mono<User> save(String email, String passwordHash);

    Mono<User> findByEmail(String email);

    Mono<User> findById(Long userId);

    Mono<Boolean> existsByEmail(String email);
}
