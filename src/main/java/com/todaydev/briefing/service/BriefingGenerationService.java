package com.todaydev.briefing.service;

import com.todaydev.ai.AiSummaryResult;
import com.todaydev.ai.AiSummaryService;
import com.todaydev.briefing.domain.ApiCallLog;
import com.todaydev.briefing.domain.ApiCallStatus;
import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.BriefingItem;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.domain.Source;
import com.todaydev.briefing.progress.BriefingProgressPayload;
import com.todaydev.briefing.progress.BriefingProgressService;
import com.todaydev.briefing.progress.BriefingStreamMessage;
import com.todaydev.briefing.progress.ProgressStep;
import com.todaydev.briefing.repository.BriefingRepository;
import com.todaydev.common.config.properties.BriefingProperties;
import com.todaydev.external.ExternalArticle;
import com.todaydev.notification.service.NotificationEnqueueService;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import com.todaydev.preference.repository.PreferenceRepository;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
public class BriefingGenerationService {

    private static final Logger log = LoggerFactory.getLogger(BriefingGenerationService.class);
    private static final String DEFAULT_TITLE = "Todaydev briefing";

    private final BriefingLockService lockService;
    private final PreferenceRepository preferenceRepository;
    private final BriefingRepository briefingRepository;
    private final ExternalArticleCollector articleCollector;
    private final AiSummaryService aiSummaryService;
    private final BriefingCandidateFactory candidateFactory;
    private final BriefingDeduplicator deduplicator;
    private final BriefingProgressService progressService;
    private final NotificationEnqueueService notificationEnqueueService;
    private final BriefingProperties.Collection collection;

    public BriefingGenerationService(
            BriefingLockService lockService,
            PreferenceRepository preferenceRepository,
            BriefingRepository briefingRepository,
            ExternalArticleCollector articleCollector,
            AiSummaryService aiSummaryService,
            BriefingCandidateFactory candidateFactory,
            BriefingDeduplicator deduplicator,
            BriefingProgressService progressService,
            NotificationEnqueueService notificationEnqueueService,
            BriefingProperties properties
    ) {
        this.lockService = lockService;
        this.preferenceRepository = preferenceRepository;
        this.briefingRepository = briefingRepository;
        this.articleCollector = articleCollector;
        this.aiSummaryService = aiSummaryService;
        this.candidateFactory = candidateFactory;
        this.deduplicator = deduplicator;
        this.progressService = progressService;
        this.notificationEnqueueService = notificationEnqueueService;
        this.collection = properties.collection();
    }

    public Mono<Void> process(BriefingGenerationJob job) {
        return Mono.usingWhen(
                Mono.just(job.lock()),
                ignored -> generate(job)
                        .onErrorResume(error -> markFailed(job.briefingId())),
                lockService::release,
                (lock, error) -> lockService.release(lock),
                lockService::release
        ).then();
    }

    private Mono<Void> generate(BriefingGenerationJob job) {
        Mono<List<InterestKeyword>> keywords = preferenceRepository.findKeywordsByUserId(job.userId()).collectList();
        Mono<List<WatchedRepository>> repositories = preferenceRepository.findRepositoriesByUserId(job.userId()).collectList();

        return Mono.zip(keywords, repositories)
                .flatMap(preferences -> collectAndSave(job.briefingId(), preferences))
                .then();
    }

    private Mono<Briefing> collectAndSave(
            Long briefingId,
            Tuple2<List<InterestKeyword>, List<WatchedRepository>> preferences
    ) {
        List<InterestKeyword> keywords = preferences.getT1();
        List<WatchedRepository> repositories = preferences.getT2();

        return collectingStarted(briefingId)
                .then(articleCollector.collect(briefingId, keywords, repositories))
                .flatMap(results -> collected(briefingId, results)
                        .then(Mono.defer(() -> {
                            List<ApiCallLog> logs = results.stream()
                                    .map(SourceCollectionResult::apiCallLog)
                                    .toList();
                            List<ExternalArticle> articles = results.stream()
                                    .flatMap(result -> result.articles().stream())
                                    .toList();
                            List<Source> failedSources = results.stream()
                                    .filter(result -> result.apiCallLog().status() == ApiCallStatus.FAILED)
                                    .map(SourceCollectionResult::source)
                                    .toList();
                            BriefingStatus status = resolveCollectionStatus(results);

                            return publishProgress(briefingId, ProgressStep.FILTERING, null, null, null,
                                    "Filtering collected articles")
                                    .then(publishProgress(briefingId, ProgressStep.SCORING, null, null, null,
                                            "Scoring briefing candidates"))
                                    .then(createCandidates(articles, keywords, repositories))
                                    .map(deduplicator::deduplicate)
                                    .flatMap(candidates -> saveResult(briefingId, status, candidates, logs, failedSources));
                        })));
    }

    private Mono<List<BriefingCandidate>> createCandidates(
            List<ExternalArticle> articles,
            List<InterestKeyword> keywords,
            List<WatchedRepository> repositories
    ) {
        return Flux.fromIterable(articles)
                .flatMap(article -> Mono.fromSupplier(() -> candidateFactory.create(article, keywords, repositories)),
                        collection.candidateConcurrency())
                .collectList();
    }

    private Mono<Briefing> saveResult(
            Long briefingId,
            BriefingStatus collectionStatus,
            List<BriefingCandidate> candidates,
            List<ApiCallLog> sourceLogs,
            List<Source> failedSources
    ) {
        List<BriefingItem> items = candidates.stream()
                .map(candidate -> candidate.toItem(briefingId))
                .toList();

        return publishProgress(briefingId, ProgressStep.SAVING, null, null, null, "Saving briefing items")
                .then(briefingRepository.saveApiCallLogs(sourceLogs))
                .thenMany(briefingRepository.saveItems(briefingId, items))
                .then(publishProgress(briefingId, ProgressStep.AI_SUMMARIZING, Source.AI, null, null,
                        "Generating AI summary"))
                .then(aiSummaryService.summarize(briefingId, candidates))
                .flatMap(aiSummary -> briefingRepository.saveApiCallLogs(List.of(aiSummary.apiCallLog()))
                        .then(briefingRepository.updateStatus(
                                briefingId,
                                resolveFinalStatus(collectionStatus, aiSummary),
                                DEFAULT_TITLE,
                                aiSummary.summary()
                        )))
                .flatMap(briefing -> publishTerminal(briefing, failedSources)
                        .then(notificationEnqueueService.enqueueForBriefing(briefing)
                                .onErrorResume(error -> {
                                    log.warn(
                                            "Notification enqueue failed: briefingId={}, userId={}",
                                            briefing.briefingId(), briefing.userId(), error
                                    );
                                    return Mono.empty();
                                }))
                        .thenReturn(briefing));
    }

    private Mono<Void> collectingStarted(Long briefingId) {
        return publishProgress(briefingId, ProgressStep.GITHUB_COLLECTING, Source.GITHUB, null, null,
                "Collecting GitHub releases")
                .then(publishProgress(briefingId, ProgressStep.HACKER_NEWS_COLLECTING, Source.HACKER_NEWS, null, null,
                        "Collecting Hacker News stories"))
                .then(publishProgress(briefingId, ProgressStep.DEVTO_COLLECTING, Source.DEVTO, null, null,
                        "Collecting DEV.to articles"));
    }

    private Mono<Void> collected(Long briefingId, List<SourceCollectionResult> results) {
        return Flux.fromIterable(results)
                .concatMap(result -> publishProgress(
                        briefingId,
                        switch (result.source()) {
                            case GITHUB -> ProgressStep.GITHUB_COLLECTED;
                            case HACKER_NEWS -> ProgressStep.HACKER_NEWS_COLLECTED;
                            case DEVTO -> ProgressStep.DEVTO_COLLECTED;
                            case AI -> throw new IllegalArgumentException("AI is not an article source");
                        },
                        result.source(),
                        result.articles().size(),
                        result.articles().size(),
                        "Source collection completed"
                ))
                .then();
    }

    private Mono<Briefing> publishTerminal(Briefing briefing, List<Source> collectionFailures) {
        List<Source> failedSources = briefing.status() == BriefingStatus.SUMMARY_FAILED
                ? Stream.concat(collectionFailures.stream(), Stream.of(Source.AI)).distinct().toList()
                : collectionFailures;
        String eventName = switch (briefing.status()) {
            case COMPLETED -> BriefingStreamMessage.DONE_EVENT;
            case PARTIAL, SUMMARY_FAILED -> BriefingStreamMessage.PARTIAL_DONE_EVENT;
            case FAILED -> BriefingStreamMessage.FAILED_EVENT;
            case GENERATING -> BriefingStreamMessage.PROGRESS_EVENT;
        };
        ProgressStep step = switch (briefing.status()) {
            case COMPLETED -> ProgressStep.DONE;
            case PARTIAL, SUMMARY_FAILED -> ProgressStep.PARTIAL_DONE;
            case FAILED -> ProgressStep.FAILED;
            case GENERATING -> ProgressStep.BRIEFING_REQUESTED;
        };
        BriefingProgressPayload payload = BriefingProgressPayload.terminal(
                briefing.briefingId(), step, briefing.status(), "Briefing generation finished", failedSources);

        return progressService.publish(new BriefingStreamMessage(eventName, payload)).thenReturn(briefing);
    }

    private Mono<Void> markFailed(Long briefingId) {
        return briefingRepository.updateStatus(briefingId, BriefingStatus.FAILED, DEFAULT_TITLE,
                        "Briefing generation failed")
                .flatMap(briefing -> publishTerminal(briefing, List.of()))
                .then();
    }

    private Mono<Void> publishProgress(
            Long briefingId,
            ProgressStep step,
            Source source,
            Integer processed,
            Integer total,
            String message
    ) {
        return progressService.publish(new BriefingStreamMessage(
                BriefingStreamMessage.PROGRESS_EVENT,
                BriefingProgressPayload.progress(briefingId, step, source, processed, total, message)
        ));
    }

    private BriefingStatus resolveCollectionStatus(List<SourceCollectionResult> results) {
        long failures = results.stream()
                .filter(result -> result.apiCallLog().status() == ApiCallStatus.FAILED)
                .count();

        if (failures == 0) {
            return BriefingStatus.COMPLETED;
        }
        if (failures == results.size()) {
            return BriefingStatus.FAILED;
        }
        return BriefingStatus.PARTIAL;
    }

    private BriefingStatus resolveFinalStatus(BriefingStatus collectionStatus, AiSummaryResult aiSummary) {
        if (collectionStatus == BriefingStatus.FAILED) {
            return BriefingStatus.FAILED;
        }
        if (!aiSummary.success()) {
            return BriefingStatus.SUMMARY_FAILED;
        }
        return collectionStatus;
    }
}
