package com.todaydev.external;

import com.todaydev.common.config.properties.ExternalApiProperties;

public final class ExternalApiPropertiesFactory {

    private ExternalApiPropertiesFactory() {
    }

    public static ExternalApiProperties properties(String baseUrl, long timeoutMillis) {
        return propertiesWithGeminiKey(baseUrl, timeoutMillis, "");
    }

    public static ExternalApiProperties propertiesWithGeminiKey(String baseUrl, long timeoutMillis, String geminiKey) {
        return new ExternalApiProperties(
                new ExternalApiProperties.Client(timeoutMillis, 1, 0),
                new ExternalApiProperties.GitHub("", baseUrl),
                new ExternalApiProperties.HackerNews(baseUrl),
                new ExternalApiProperties.DevTo(baseUrl),
                new ExternalApiProperties.Gemini(geminiKey, baseUrl, "gemini-2.5-flash-lite")
        );
    }
}
