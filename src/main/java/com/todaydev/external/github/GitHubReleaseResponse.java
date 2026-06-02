package com.todaydev.external.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

record GitHubReleaseResponse(
        Long id,
        String name,
        @JsonProperty("tag_name") String tagName,
        @JsonProperty("html_url") String htmlUrl,
        String body,
        @JsonProperty("published_at") OffsetDateTime publishedAt
) {

    String displayTitle() {
        if (name != null && !name.isBlank()) {
            return name;
        }
        return tagName == null || tagName.isBlank() ? "GitHub Release" : tagName;
    }

    String externalId() {
        return id == null ? tagName : String.valueOf(id);
    }
}
