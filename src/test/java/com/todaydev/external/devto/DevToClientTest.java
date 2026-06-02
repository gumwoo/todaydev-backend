package com.todaydev.external.devto;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.external.ExternalApiPropertiesFactory;
import com.todaydev.external.ExternalSource;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class DevToClientTest {

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
    void fetchArticlesByTag_mapsDevToResponseToExternalArticle() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "id": 10,
                            "title": "WebFlux tips",
                            "description": "Reactive programming notes",
                            "url": "https://dev.to/example/webflux-tips",
                            "published_at": "2026-05-21T00:00:00Z",
                            "tag_list": ["java", "spring"],
                            "public_reactions_count": 7,
                            "comments_count": 2
                          }
                        ]
                        """));

        DevToClient client = new DevToClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                ExternalApiPropertiesFactory.properties(server.url("/").toString(), 1000)
        );

        StepVerifier.create(client.fetchArticlesByTag(" Java ", 5))
                .assertNext(article -> {
                    assertThat(article.source()).isEqualTo(ExternalSource.DEVTO);
                    assertThat(article.externalId()).isEqualTo("10");
                    assertThat(article.title()).isEqualTo("WebFlux tips");
                    assertThat(article.metadata()).containsEntry("comments", 2);
                })
                .verifyComplete();
    }

    @Test
    void fetchArticlesByTag_mapsSlowResponseToTimeout() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[]")
                .setBodyDelay(300, TimeUnit.MILLISECONDS));

        DevToClient client = new DevToClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                ExternalApiPropertiesFactory.properties(server.url("/").toString(), 100)
        );

        StepVerifier.create(client.fetchArticlesByTag("java", 5))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode()).isEqualTo(ErrorCode.EXTERNAL_TIMEOUT);
                })
                .verify();
    }
}
