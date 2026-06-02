package com.todaydev.ai;

import java.util.List;

record GeminiGenerateContentRequest(
        List<GeminiContent> contents
) {

    static GeminiGenerateContentRequest text(String prompt) {
        return new GeminiGenerateContentRequest(List.of(
                new GeminiContent(List.of(new GeminiPart(prompt)))
        ));
    }

    record GeminiContent(List<GeminiPart> parts) {
    }

    record GeminiPart(String text) {
    }
}
