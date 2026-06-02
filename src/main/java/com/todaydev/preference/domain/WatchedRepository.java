package com.todaydev.preference.domain;

import java.time.LocalDateTime;

public record WatchedRepository(
        Long repositoryId,
        Long userId,
        String owner,
        String repoName,
        LocalDateTime createdAt
) {
}
