package com.todaydev.external.github;

public record GitHubRepositoryReference(
        String owner,
        String repoName
) {

    public GitHubRepositoryReference {
        owner = owner == null ? "" : owner.trim();
        repoName = repoName == null ? "" : repoName.trim();
    }

    public boolean valid() {
        return !owner.isBlank() && !repoName.isBlank();
    }
}
