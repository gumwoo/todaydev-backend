package com.todaydev.briefing.domain;

import com.todaydev.external.ExternalSource;

public enum Source {
    GITHUB,
    HACKER_NEWS,
    DEVTO,
    AI;

    public static Source from(ExternalSource source) {
        return switch (source) {
            case GITHUB -> GITHUB;
            case HACKER_NEWS -> HACKER_NEWS;
            case DEVTO -> DEVTO;
        };
    }
}
