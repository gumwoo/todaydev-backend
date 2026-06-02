package com.todaydev.preference.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRepositoryRequest(
        @NotBlank @Size(max = 100) String owner,
        @NotBlank @Size(max = 100) String repoName
) {
}
