package com.todaydev.savedarticle.web;

import jakarta.validation.constraints.Size;

public record UpdateSavedArticleRequest(
        @Size(max = 1000) String memo
) {
}
