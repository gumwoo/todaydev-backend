package com.todaydev.briefing.service;

import com.todaydev.briefing.domain.BriefingCandidate;
import com.todaydev.briefing.domain.MatchedKeyword;
import com.todaydev.briefing.domain.Source;
import com.todaydev.external.ExternalArticle;
import com.todaydev.preference.domain.InterestKeyword;
import com.todaydev.preference.domain.WatchedRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BriefingCandidateFactory {

    private final KeywordMatcher keywordMatcher;
    private final WatchedRepositoryMatcher watchedRepositoryMatcher;
    private final BriefingScorer scorer;

    public BriefingCandidateFactory(
            KeywordMatcher keywordMatcher,
            WatchedRepositoryMatcher watchedRepositoryMatcher,
            BriefingScorer scorer
    ) {
        this.keywordMatcher = keywordMatcher;
        this.watchedRepositoryMatcher = watchedRepositoryMatcher;
        this.scorer = scorer;
    }

    public BriefingCandidate create(
            ExternalArticle article,
            List<InterestKeyword> keywords,
            List<WatchedRepository> repositories
    ) {
        List<MatchedKeyword> matchedKeywords = keywordMatcher.match(article, keywords);
        boolean watchedRepository = watchedRepositoryMatcher.matches(article, repositories);

        BriefingCandidate candidate = new BriefingCandidate(
                Source.from(article.source()),
                article.externalId(),
                article.title(),
                article.url(),
                article.summary(),
                article.publishedAt(),
                article.metadata(),
                matchedKeywords,
                watchedRepository,
                BigDecimal.ZERO
        );

        return candidate.withScore(scorer.score(candidate));
    }
}
