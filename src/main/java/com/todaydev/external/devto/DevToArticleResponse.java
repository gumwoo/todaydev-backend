package com.todaydev.external.devto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

record DevToArticleResponse(
        Long id,
        String title,
        String description,
        String url,
        @JsonProperty("published_at") OffsetDateTime publishedAt,
        @JsonProperty("tag_list") List<String> tagList,
        @JsonProperty("public_reactions_count") Integer publicReactionsCount,
        @JsonProperty("comments_count") Integer commentsCount
) {
}
