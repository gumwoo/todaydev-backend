package com.todaydev.preference.repository;

import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcPreferenceRepository implements PreferenceRepository {

    private final DatabaseClient databaseClient;

    public R2dbcPreferenceRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Flux<InterestKeyword> findKeywordsByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT interest_id, user_id, keyword, weight, created_at
                        FROM user_interest
                        WHERE user_id = :userId
                        ORDER BY created_at ASC, interest_id ASC
                        """)
                .bind("userId", userId)
                .map(this::mapKeyword)
                .all();
    }

    @Override
    public Mono<InterestKeyword> saveKeyword(Long userId, String keyword, int weight) {
        return databaseClient.sql("""
                        INSERT INTO user_interest (user_id, keyword, weight)
                        VALUES (:userId, :keyword, :weight)
                        RETURNING interest_id, user_id, keyword, weight, created_at
                        """)
                .bind("userId", userId)
                .bind("keyword", keyword)
                .bind("weight", weight)
                .map(this::mapKeyword)
                .one();
    }

    @Override
    public Mono<Boolean> deleteKeyword(Long userId, Long keywordId) {
        return databaseClient.sql("""
                        DELETE FROM user_interest
                        WHERE user_id = :userId
                          AND interest_id = :keywordId
                        """)
                .bind("userId", userId)
                .bind("keywordId", keywordId)
                .fetch()
                .rowsUpdated()
                .map(count -> count > 0);
    }

    @Override
    public Flux<WatchedRepository> findRepositoriesByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT repo_id, user_id, owner, repo_name, created_at
                        FROM watched_repository
                        WHERE user_id = :userId
                        ORDER BY created_at ASC, repo_id ASC
                        """)
                .bind("userId", userId)
                .map(this::mapRepository)
                .all();
    }

    @Override
    public Mono<WatchedRepository> saveRepository(Long userId, String owner, String repoName) {
        return databaseClient.sql("""
                        INSERT INTO watched_repository (user_id, owner, repo_name)
                        VALUES (:userId, :owner, :repoName)
                        RETURNING repo_id, user_id, owner, repo_name, created_at
                        """)
                .bind("userId", userId)
                .bind("owner", owner)
                .bind("repoName", repoName)
                .map(this::mapRepository)
                .one();
    }

    @Override
    public Mono<Boolean> deleteRepository(Long userId, Long repositoryId) {
        return databaseClient.sql("""
                        DELETE FROM watched_repository
                        WHERE user_id = :userId
                          AND repo_id = :repositoryId
                        """)
                .bind("userId", userId)
                .bind("repositoryId", repositoryId)
                .fetch()
                .rowsUpdated()
                .map(count -> count > 0);
    }

    private InterestKeyword mapKeyword(Row row, RowMetadata metadata) {
        return new InterestKeyword(
                row.get("interest_id", Long.class),
                row.get("user_id", Long.class),
                row.get("keyword", String.class),
                row.get("weight", Integer.class),
                row.get("created_at", LocalDateTime.class)
        );
    }

    private WatchedRepository mapRepository(Row row, RowMetadata metadata) {
        return new WatchedRepository(
                row.get("repo_id", Long.class),
                row.get("user_id", Long.class),
                row.get("owner", String.class),
                row.get("repo_name", String.class),
                row.get("created_at", LocalDateTime.class)
        );
    }
}
