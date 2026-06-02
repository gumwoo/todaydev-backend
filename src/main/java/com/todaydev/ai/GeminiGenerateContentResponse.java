package com.todaydev.ai;

import java.util.List;

record GeminiGenerateContentResponse(
        List<Candidate> candidates
) {

    String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }

        Content content = candidates.get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return "";
        }

        String text = content.parts().get(0).text();
        return text == null ? "" : text.trim();
    }

    record Candidate(Content content) {
    }

    record Content(List<Part> parts) {
    }

    record Part(String text) {
    }
}
