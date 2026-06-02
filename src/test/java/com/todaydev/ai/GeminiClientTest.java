package com.todaydev.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.external.ExternalApiPropertiesFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class GeminiClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void generateSummary_returnsFirstTextFromGeminiResponse() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {"text": "요약 결과"}
                                ]
                              }
                            }
                          ]
                        }
                        """));

        GeminiClient client = new GeminiClient(
                WebClient.builder()
                        .baseUrl(server.url("/").toString())
                        .defaultHeader("x-goog-api-key", "test-key")
                        .build(),
                ExternalApiPropertiesFactory.propertiesWithGeminiKey(server.url("/").toString(), 10000, "test-key")
        );

        StepVerifier.create(client.generateSummary("prompt"))
                .expectNext("요약 결과")
                .verifyComplete();
    }

    @Test
    void generateSummary_failsSafely_whenApiKeyIsMissing() {
        GeminiClient client = new GeminiClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                ExternalApiPropertiesFactory.properties(server.url("/").toString(), 1000)
        );

        StepVerifier.create(client.generateSummary("prompt"))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode()).isEqualTo(ErrorCode.AI_SUMMARY_FAILED);
                })
                .verify();
    }
}
