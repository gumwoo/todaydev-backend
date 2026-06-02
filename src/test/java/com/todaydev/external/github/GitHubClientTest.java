package com.todaydev.external.github;

import static org.assertj.core.api.Assertions.assertThat;

import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.external.ExternalApiPropertiesFactory;
import com.todaydev.external.ExternalArticle;
import com.todaydev.external.ExternalSource;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class GitHubClientTest {

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
    void fetchRepositoryReleases_mapsGithubResponseToExternalArticle() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        [
                          {
                            "id": 100,
                            "name": "Spring Framework 7.0",
                            "tag_name": "v7.0.0",
                            "html_url": "https://github.com/spring-projects/spring-framework/releases/tag/v7.0.0",
                            "body": "Release notes",
                            "published_at": "2026-05-21T00:00:00Z"
                          }
                        ]
                        """));

        GitHubClient client = new GitHubClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                ExternalApiPropertiesFactory.properties(server.url("/").toString(), 1000)
        );

        StepVerifier.create(client.fetchRepositoryReleases("spring-projects", "spring-framework", 5))
                .assertNext(article -> {
                    assertThat(article.source()).isEqualTo(ExternalSource.GITHUB);
                    assertThat(article.externalId()).isEqualTo("100");
                    assertThat(article.title()).isEqualTo("Spring Framework 7.0");
                    assertThat(article.metadata()).containsEntry("owner", "spring-projects");
                })
                .verifyComplete();
    }

    @Test
    void fetchRepositoryReleases_mapsRateLimitToCommonErrorCode() {
        server.enqueue(new MockResponse().setResponseCode(429));

        GitHubClient client = new GitHubClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                ExternalApiPropertiesFactory.properties(server.url("/").toString(), 1000)
        );

        StepVerifier.create(client.fetchRepositoryReleases("owner", "repo", 5))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode()).isEqualTo(ErrorCode.EXTERNAL_RATE_LIMITED);
                })
                .verify();
    }
}
