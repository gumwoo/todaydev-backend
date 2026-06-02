package com.todaydev.preference.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKeywordRequest(
        @NotBlank @Size(max = 100) String keyword,
        @Min(1) @Max(10) Integer weight
) {
}
