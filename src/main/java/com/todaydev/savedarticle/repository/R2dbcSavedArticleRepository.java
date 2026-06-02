package com.todaydev.savedarticle.repository;

import com.todaydev.briefing.domain.Source;
import com.todaydev.savedarticle.domain.SavedArticle;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.LocalDateTime;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcSavedArticleRepository implements SavedArticleRepository {

    private final DatabaseClient databaseClient;

    public R2dbcSavedArticleRepository(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<SavedArticle> save(Long userId, Long itemId, String memo) {
        return databaseClient.sql("""
                        WITH inserted AS (
                            INSERT INTO saved_article (user_id, item_id, memo)
                            SELECT :userId, bi.item_id, :memo
                            FROM briefing_item bi
                            JOIN briefing b ON b.briefing_id = bi.briefing_id
                            WHERE bi.item_id = :itemId
                              AND b.user_id = :userId
                            RETURNING saved_id, user_id, item_id, memo, created_at
                        )
                        SELECT inserted.saved_id, inserted.user_id, inserted.item_id,
                               bi.title, bi.url, bi.source, inserted.memo, inserted.created_at
                        FROM inserted
                        JOIN briefing_item bi ON bi.item_id = inserted.item_id
                        """)
                .bind("userId", userId)
                .bind("itemId", itemId)
                .bind("memo", memo)
                .map(this::mapSavedArticle)
                .one();
    }

    @Override
    public Flux<SavedArticle> findByUserId(Long userId, int page, int size) {
        return databaseClient.sql("""
                        SELECT sa.saved_id, sa.user_id, sa.item_id,
                               bi.title, bi.url, bi.source, sa.memo, sa.created_at
                        FROM saved_article sa
                        JOIN briefing_item bi ON bi.item_id = sa.item_id
                        WHERE sa.user_id = :userId
                        ORDER BY sa.created_at DESC, sa.saved_id DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .bind("userId", userId)
                .bind("limit", size)
                .bind("offset", page * size)
                .map(this::mapSavedArticle)
                .all();
    }

    @Override
    public Mono<Long> countByUserId(Long userId) {
        return databaseClient.sql("""
                        SELECT COUNT(*) AS total_count
                        FROM saved_article
                        WHERE user_id = :userId
                        """)
                .bind("userId", userId)
                .map(row -> row.get("total_count", Long.class))
                .one();
    }

    @Override
    public Mono<SavedArticle> updateMemo(Long userId, Long savedId, String memo) {
        return databaseClient.sql("""
                        WITH updated AS (
                            UPDATE saved_article
                            SET memo = :memo
                            WHERE user_id = :userId
                              AND saved_id = :savedId
                            RETURNING saved_id, user_id, item_id, memo, created_at
                        )
                        SELECT updated.saved_id, updated.user_id, updated.item_id,
                               bi.title, bi.url, bi.source, updated.memo, updated.created_at
                        FROM updated
                        JOIN briefing_item bi ON bi.item_id = updated.item_id
                        """)
                .bind("userId", userId)
                .bind("savedId", savedId)
                .bind("memo", memo)
                .map(this::mapSavedArticle)
                .one();
    }

    @Override
    public Mono<Boolean> delete(Long userId, Long savedId) {
        return databaseClient.sql("""
                        DELETE FROM saved_article
                        WHERE user_id = :userId
                          AND saved_id = :savedId
                        """)
                .bind("userId", userId)
                .bind("savedId", savedId)
                .fetch()
                .rowsUpdated()
                .map(count -> count > 0);
    }

    private SavedArticle mapSavedArticle(Row row, RowMetadata metadata) {
        return new SavedArticle(
                row.get("saved_id", Long.class),
                row.get("user_id", Long.class),
                row.get("item_id", Long.class),
                row.get("title", String.class),
                row.get("url", String.class),
                Source.valueOf(row.get("source", String.class)),
                row.get("memo", String.class),
                row.get("created_at", LocalDateTime.class)
        );
    }
}
