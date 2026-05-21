package com.todaydev.auth.repository;

import com.todaydev.auth.domain.User;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcUserRepository implements UserRepository {

    private final DatabaseClient databaseClient;

    public R2dbcUserRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<User> save(String email, String passwordHash) {
        return databaseClient.sql("""
                        INSERT INTO users (email, password_hash)
                        VALUES (:email, :passwordHash)
                        RETURNING user_id, email, password_hash, created_at
                        """)
                .bind("email", email)
                .bind("passwordHash", passwordHash)
                .map(this::mapUser)
                .one();
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return databaseClient.sql("""
                        SELECT user_id, email, password_hash, created_at
                        FROM users
                        WHERE email = :email
                        """)
                .bind("email", email)
                .map(this::mapUser)
                .one();
    }

    @Override
    public Mono<User> findById(Long userId) {
        return databaseClient.sql("""
                        SELECT user_id, email, password_hash, created_at
                        FROM users
                        WHERE user_id = :userId
                        """)
                .bind("userId", userId)
                .map(this::mapUser)
                .one();
    }

    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return databaseClient.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM users
                            WHERE email = :email
                        ) AS exists
                        """)
                .bind("email", email)
                .map((row, metadata) -> Boolean.TRUE.equals(row.get("exists", Boolean.class)))
                .one();
    }

    private User mapUser(Row row, RowMetadata metadata) {
        return new User(
                row.get("user_id", Long.class),
                row.get("email", String.class),
                row.get("password_hash", String.class),
                row.get("created_at", LocalDateTime.class)
        );
    }
}
