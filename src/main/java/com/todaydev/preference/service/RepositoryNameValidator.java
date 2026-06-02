package com.todaydev.preference.service;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.util.regex.Pattern;

public final class RepositoryNameValidator {

    private static final Pattern OWNER_PATTERN = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?$");
    private static final Pattern REPO_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    private RepositoryNameValidator() {
    }

    public static String normalizeOwner(String owner) {
        String normalized = normalize(owner);
        if (!OWNER_PATTERN.matcher(normalized).matches()) {
            throw new TodaydevException(ErrorCode.PREFERENCE_REPOSITORY_FORMAT_INVALID);
        }
        return normalized;
    }

    public static String normalizeRepoName(String repoName) {
        String normalized = normalize(repoName);
        if (!REPO_PATTERN.matcher(normalized).matches()
                || normalized.startsWith(".")
                || normalized.endsWith(".")) {
            throw new TodaydevException(ErrorCode.PREFERENCE_REPOSITORY_FORMAT_INVALID);
        }
        return normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            throw new TodaydevException(ErrorCode.PREFERENCE_REPOSITORY_FORMAT_INVALID);
        }
        return value.trim();
    }
}
