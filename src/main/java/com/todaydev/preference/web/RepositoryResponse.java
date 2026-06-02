package com.todaydev.preference.web;

import java.time.LocalDateTime;

public record RepositoryResponse(
        Long repositoryId,
        String owner,
        String repoName,
        LocalDateTime createdAt
) {
}
