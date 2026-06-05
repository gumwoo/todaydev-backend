package com.todaydev.external.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record GitHubRepositorySearchResponse(
        List<Item> items
) {

    record Item(
            Owner owner,
            String name,
            @JsonProperty("full_name") String fullName
    ) {

        GitHubRepositoryReference toReference() {
            if (owner != null && owner.login() != null && name != null) {
                return new GitHubRepositoryReference(owner.login(), name);
            }

            if (fullName == null || !fullName.contains("/")) {
                return new GitHubRepositoryReference("", "");
            }

            String[] parts = fullName.split("/", 2);
            return new GitHubRepositoryReference(parts[0], parts[1]);
        }
    }

    record Owner(
            String login
    ) {
    }
}
