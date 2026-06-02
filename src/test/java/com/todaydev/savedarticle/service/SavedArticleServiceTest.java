package com.todaydev.savedarticle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.todaydev.briefing.domain.Source;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import com.todaydev.savedarticle.domain.SavedArticle;
import com.todaydev.savedarticle.repository.SavedArticleRepository;
import com.todaydev.savedarticle.web.SaveArticleRequest;
import com.todaydev.savedarticle.web.UpdateSavedArticleRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class SavedArticleServiceTest {

    @Test
    void save_returnsSavedArticle_whenItemBelongsToUser() {
        SavedArticleRepository repository = mock(SavedArticleRepository.class);
        SavedArticleService service = new SavedArticleService(repository);
        SavedArticle savedArticle = savedArticle(1L, 10L, "memo");
        when(repository.save(1L, 10L, "memo")).thenReturn(Mono.just(savedArticle));

        StepVerifier.create(service.save(1L, 10L, new SaveArticleRequest(" memo ")))
                .assertNext(response -> {
                    assertThat(response.savedId()).isEqualTo(1L);
                    assertThat(response.itemId()).isEqualTo(10L);
                    assertThat(response.memo()).isEqualTo("memo");
                })
                .verifyComplete();
    }

    @Test
    void save_returnsConflict_whenAlreadySaved() {
        SavedArticleRepository repository = mock(SavedArticleRepository.class);
        SavedArticleService service = new SavedArticleService(repository);
        when(repository.save(eq(1L), eq(10L), eq("")))
                .thenReturn(Mono.error(new DuplicateKeyException("duplicated")));

        StepVerifier.create(service.save(1L, 10L, new SaveArticleRequest(null)))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode())
                            .isEqualTo(ErrorCode.SAVED_ARTICLE_DUPLICATED);
                })
                .verify();
    }

    @Test
    void save_returnsNotFound_whenItemDoesNotBelongToUser() {
        SavedArticleRepository repository = mock(SavedArticleRepository.class);
        SavedArticleService service = new SavedArticleService(repository);
        when(repository.save(1L, 999L, "")).thenReturn(Mono.empty());

        StepVerifier.create(service.save(1L, 999L, new SaveArticleRequest("")))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode())
                            .isEqualTo(ErrorCode.SAVED_ARTICLE_NOT_FOUND);
                })
                .verify();
    }

    @Test
    void findMySavedArticles_returnsPagedResponse() {
        SavedArticleRepository repository = mock(SavedArticleRepository.class);
        SavedArticleService service = new SavedArticleService(repository);
        when(repository.findByUserId(1L, 0, 2)).thenReturn(Flux.just(
                savedArticle(1L, 10L, "one"),
                savedArticle(2L, 11L, "two")
        ));
        when(repository.countByUserId(1L)).thenReturn(Mono.just(3L));

        StepVerifier.create(service.findMySavedArticles(1L, 0, 2))
                .assertNext(response -> {
                    assertThat(response.items()).hasSize(2);
                    assertThat(response.totalElements()).isEqualTo(3);
                    assertThat(response.totalPages()).isEqualTo(2);
                    assertThat(response.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void updateMemo_returnsNotFound_whenSavedArticleDoesNotExist() {
        SavedArticleRepository repository = mock(SavedArticleRepository.class);
        SavedArticleService service = new SavedArticleService(repository);
        when(repository.updateMemo(1L, 100L, "new memo")).thenReturn(Mono.empty());

        StepVerifier.create(service.updateMemo(1L, 100L, new UpdateSavedArticleRequest("new memo")))
                .expectErrorSatisfies(throwable -> {
                    assertThat(throwable).isInstanceOf(TodaydevException.class);
                    assertThat(((TodaydevException) throwable).errorCode())
                            .isEqualTo(ErrorCode.SAVED_ARTICLE_NOT_FOUND);
                })
                .verify();
    }

    @Test
    void delete_returnsDeletedResponse_whenDeleted() {
        SavedArticleRepository repository = mock(SavedArticleRepository.class);
        SavedArticleService service = new SavedArticleService(repository);
        when(repository.delete(1L, 100L)).thenReturn(Mono.just(true));

        StepVerifier.create(service.delete(1L, 100L))
                .assertNext(response -> assertThat(response.deleted()).isTrue())
                .verifyComplete();
    }

    private SavedArticle savedArticle(Long savedId, Long itemId, String memo) {
        return new SavedArticle(
                savedId,
                1L,
                itemId,
                "Spring Framework Release",
                "https://github.com/spring-projects/spring-framework",
                Source.GITHUB,
                memo,
                LocalDateTime.now()
        );
    }
}
