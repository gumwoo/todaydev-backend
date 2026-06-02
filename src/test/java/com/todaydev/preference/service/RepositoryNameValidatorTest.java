package com.todaydev.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import org.junit.jupiter.api.Test;

class RepositoryNameValidatorTest {

    @Test
    void normalizeOwner_acceptsValidGithubOwner() {
        assertThat(RepositoryNameValidator.normalizeOwner(" spring-projects "))
                .isEqualTo("spring-projects");
    }

    @Test
    void normalizeOwner_rejectsOwnerEndingWithDash() {
        assertThatThrownBy(() -> RepositoryNameValidator.normalizeOwner("spring-"))
                .isInstanceOf(TodaydevException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PREFERENCE_REPOSITORY_FORMAT_INVALID);
    }

    @Test
    void normalizeRepoName_acceptsValidGithubRepoName() {
        assertThat(RepositoryNameValidator.normalizeRepoName(" spring-framework "))
                .isEqualTo("spring-framework");
    }

    @Test
    void normalizeRepoName_rejectsRepoNameEndingWithDot() {
        assertThatThrownBy(() -> RepositoryNameValidator.normalizeRepoName("repo."))
                .isInstanceOf(TodaydevException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PREFERENCE_REPOSITORY_FORMAT_INVALID);
    }
}
