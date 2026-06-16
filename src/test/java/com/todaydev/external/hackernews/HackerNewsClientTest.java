package com.todaydev.external.hackernews;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.external.ExternalApiCacheTestSupport;
import com.todaydev.external.ExternalApiPropertiesFactory;
import com.todaydev.external.ExternalSource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class HackerNewsClientTest {

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
    void fetchTopStories_fetchesStoryIdsAndMapsItems() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[1]"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": 1,
                          "title": "A useful engineering post",
                          "url": "https://example.com/post",
                          "by": "hn-user",
                          "score": 42,
                          "time": 1779408000,
                          "type": "story"
                        }
                        """));

        HackerNewsClient client = new HackerNewsClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                ExternalApiPropertiesFactory.properties(server.url("/").toString(), 1000),
                ExternalApiCacheTestSupport.passthroughCache()
        );

        StepVerifier.create(client.fetchTopStories(1))
                .assertNext(article -> {
                    assertThat(article.source()).isEqualTo(ExternalSource.HACKER_NEWS);
                    assertThat(article.externalId()).isEqualTo("1");
                    assertThat(article.title()).isEqualTo("A useful engineering post");
                    assertThat(article.metadata()).containsEntry("score", 42);
                })
                .verifyComplete();
    }
}
