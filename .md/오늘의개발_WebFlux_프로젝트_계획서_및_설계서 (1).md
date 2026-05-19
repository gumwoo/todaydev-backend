# 오늘의 개발

## WebFlux 기반 개발자 맞춤 AI 브리핑 서비스

**프로젝트 계획서 및 설계서**

> **문서 목적**  
> 본 문서는 백엔드 포트폴리오용 프로젝트 **오늘의 개발**의 기획, 기능 범위, 아키텍처, DB/API 설계, WebFlux 적용 전략, 개발 일정, 면접 어필 포인트를 정리한 설계 문서입니다.

| 항목 | 내용 |
|---|---|
| 프로젝트 유형 | 개인 포트폴리오 / 백엔드 중심 서비스 |
| 서비스 목적 | 개발자가 매일 확인해야 하는 기술 정보원을 개인 관심사 기준으로 통합 브리핑 |
| 핵심 기술 | Spring Boot, Spring WebFlux, WebClient, R2DBC, PostgreSQL, Redis, SSE, Gemini/OpenAI API |
| 핵심 결과물 | GitHub README, API 문서, 성능 비교 자료, 시연 영상/스크린샷, PDF 설계서 |
| 작성일 | 2026-05-19 |

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기획 배경 및 문제 정의](#2-기획-배경-및-문제-정의)
3. [목표 사용자와 핵심 시나리오](#3-목표-사용자와-핵심-시나리오)
4. [기능 범위](#4-기능-범위)
5. [외부 API 구성](#5-외부-api-구성)
6. [시스템 아키텍처](#6-시스템-아키텍처)
7. [WebFlux 적용 설계](#7-webflux-적용-설계)
8. [데이터베이스 설계](#8-데이터베이스-설계)
9. [API 설계](#9-api-설계)
10. [추천/점수화 설계](#10-추천점수화-설계)
11. [인증/인가 및 보안 설계](#11-인증인가-및-보안-설계)
12. [장애 대응 및 성능 전략](#12-장애-대응-및-성능-전략)
13. [테스트/검증 계획](#13-테스트검증-계획)
14. [개발 일정](#14-개발-일정)
15. [포트폴리오 어필 포인트](#15-포트폴리오-어필-포인트)
16. [프론트엔드 설계](#16-프론트엔드-설계)
17. [추천 패키지 구조](#17-추천-패키지-구조)
18. [README 구성안](#18-readme-구성안)
19. [참고 API 문서](#19-참고-api-문서)

---

## 1. 프로젝트 개요

**오늘의 개발**은 개발자가 관심 기술스택과 관심 GitHub Repository를 등록하면, GitHub, Hacker News, DEV.to 등 여러 기술 정보 API를 병렬 호출하고 AI 요약을 통해 매일 읽을 만한 개발 브리핑을 생성하는 WebFlux 기반 API Aggregation 서비스입니다.

이 프로젝트는 단순 CRUD가 아니라 외부 API 호출이 많고 응답 대기 시간이 긴 구조에서 WebFlux, WebClient, Mono/Flux, timeout/fallback, concurrency 제한, SSE 진행률 스트리밍을 실무형으로 적용하는 것을 목표로 합니다.

| 구분 | 내용 |
|---|---|
| 서비스명 | 오늘의 개발 |
| 한 줄 정의 | 관심 기술을 한눈에 모아보는 개발자 맞춤 AI 브리핑 서비스 |
| 핵심 사용자 | 백엔드 개발자, 취업/이직 준비자, 기술 트렌드를 꾸준히 확인하는 개발자 |
| 핵심 가치 | 여러 정보원을 직접 방문하지 않고 개인 관심사 기반으로 요약된 개발 정보를 확인 |
| WebFlux 사용 이유 | GitHub/Hacker News/블로그/AI API 등 여러 외부 API 호출을 병렬 처리해야 함 |

---

## 2. 기획 배경 및 문제 정의

개발자는 매일 GitHub, Hacker News, 기술 블로그, 릴리즈 노트, 개발 커뮤니티 등을 확인하지만 정보가 여러 곳에 흩어져 있고, 본인 관심 기술과 관련 없는 글도 많아 선별 비용이 큽니다.

| 문제 | 설명 | 해결 방향 |
|---|---|---|
| 정보 분산 | GitHub, Hacker News, 블로그, 커뮤니티가 분리되어 있음 | 여러 API를 통합 호출하여 하나의 브리핑 생성 |
| 선별 비용 | 관심 기술과 무관한 글이 많음 | 사용자 관심 키워드/레포 기반 필터링 |
| 읽기 부담 | 원문을 모두 읽기 어렵고 출처별 형식이 다름 | AI 요약으로 핵심 내용과 추천 이유 제공 |
| 응답 지연 | 여러 외부 API를 순차 호출하면 전체 응답이 느림 | WebFlux 기반 병렬 호출 및 timeout/fallback 적용 |
| API 장애 | 특정 출처 장애가 전체 기능 실패로 이어질 수 있음 | 부분 실패 허용, 캐시 사용, 출처별 fallback 설계 |

---

## 3. 목표 사용자와 핵심 시나리오

### 3.1 목표 사용자

- **백엔드 개발자**: Spring, Redis, PostgreSQL, MSA, WebFlux 등 관심 기술 동향을 빠르게 확인
- **취업/이직 준비자**: 면접 대비용 최신 개발 이슈와 오픈소스 흐름 파악
- **개인 프로젝트 개발자**: 관심 레포의 릴리즈/이슈를 놓치지 않고 확인

### 3.2 핵심 사용자 흐름

```text
회원가입/로그인
-> 관심 키워드 등록(Java, Spring, WebFlux, Redis 등)
-> 관심 GitHub Repository 등록(spring-projects/spring-framework 등)
-> 브리핑 생성 요청
-> 서버가 GitHub/Hacker News/DEV.to/AI API를 병렬 호출
-> 관심사 기준 필터링 및 점수화
-> AI 요약 생성
-> SSE로 진행률 표시
-> 오늘의 개발 브리핑 결과 저장/조회
```

---

## 4. 기능 범위

포트폴리오 완성도를 높이기 위해 MVP와 확장 기능을 분리합니다. MVP는 WebFlux 기반 병렬 수집, AI 요약, SSE 진행률, 브리핑 저장/조회까지를 핵심 완성 범위로 둡니다.

### 4.1 MVP 필수 기능

| 기능 | 설명 |
|---|---|
| 회원가입/로그인 | JWT 기반 인증, 비밀번호 BCrypt 해싱 |
| 관심 키워드 관리 | 사용자 관심 기술스택/키워드 등록, 수정, 삭제 |
| 관심 Repository 관리 | GitHub owner/repo 형식으로 관심 레포 등록 |
| 브리핑 생성 | 사용자 관심사 기준으로 외부 API 병렬 호출 후 브리핑 생성 |
| AI 요약 | 수집된 항목을 AI API로 요약하고 추천 이유 생성 |
| SSE 진행률 | 수집, 필터링, 요약, 저장 단계별 진행률 스트리밍 |
| 브리핑 조회 | 오늘의 브리핑, 과거 브리핑 히스토리 조회 |
| Redis 캐싱 | 외부 API 결과와 AI 요약 결과를 TTL 기반으로 캐싱 |

### 4.2 확장 기능

| 기능 | 설명 |
|---|---|
| 매일 아침 자동 생성 | 스케줄러를 통해 사용자별 일일 브리핑 자동 생성 |
| 저장한 글 관리 | 읽고 싶은 글 저장, 메모 작성 |
| 이메일/Slack 전송 | 브리핑 결과를 외부 채널로 발송 |
| 트렌드 점수 그래프 | 키워드별 노출 빈도와 증가율 시각화 |
| MSA 확장 | Gateway, Briefing Service, Notification Service 분리 |

---

## 5. 외부 API 구성

외부 API는 개인 프로젝트에서 승인 리스크가 낮고 공개 문서가 명확한 소스를 우선 사용합니다. GitHub API는 공개 데이터 조회가 가능하며 인증 요청을 사용하면 더 높은 rate limit을 받을 수 있습니다. Hacker News 공식 API는 story ID 조회 후 item 상세 조회가 필요하므로 외부 API 호출이 많은 WebFlux 학습 시나리오에 적합합니다.

| API | 용도 | MVP 사용 여부 | 비고 |
|---|---|---|---|
| GitHub REST API | 관심 레포 릴리즈/이슈, 레포 검색 | 필수 | PAT 인증 사용 권장, rate limit 헤더 기반 제어 |
| Hacker News API | top/new/show story 수집 | 필수 | ID 목록 조회 후 item 상세 병렬 조회 |
| DEV.to API | 태그 기반 개발 글 수집 | 필수 | User-Agent 헤더 필요, 공개 엔드포인트 우선. 장애/제한 발생 시 GitHub + HN 기반 partial briefing 생성, 전체 브리핑 실패로 처리하지 않음 |
| Gemini/OpenAI API | 브리핑 요약 및 추천 이유 생성 | 필수 | AI API rate limit 고려, fallback 필요 |
| Frankfurter API | 환율 부가 정보 | 확장 | 해외 SaaS/클라우드 비용, 글로벌 서비스 가격 확인용 부가 섹션. API key 불필요. MVP에서는 제외하고 확장 기능으로 추가 |

---

## 6. 시스템 아키텍처

```text
[Client]
  | REST / SSE
  v
[Spring WebFlux API Server]
  |-- Auth Module
  |-- Preference Module
  |-- Repository Watch Module
  |-- Aggregation Module
  |-- AI Summary Module
  |-- Progress Stream Module (SSE)
  |
  | WebClient
  |-- GitHub API
  |-- Hacker News API
  |-- DEV.to API
  |-- Gemini/OpenAI API
  (확장: Frankfurter API 추가 시 WebClient Adapter로 연결)
  |
  | R2DBC                         | Reactive Redis
  v                               v
[PostgreSQL]                   [Redis]
                                - API response cache
                                - AI summary cache
                                - Refresh token
                                - Progress event buffer
```

| 계층 | 역할 |
|---|---|
| Controller | REST API와 SSE 스트림 엔드포인트 제공 |
| Security Filter | JWT 검증 및 인증 정보 주입 |
| Application Service | 브리핑 생성 유스케이스 조합 |
| Client Adapter | WebClient 기반 외부 API 호출 |
| Domain | Briefing, BriefingItem, InterestKeyword 등 핵심 규칙 |
| Repository | R2DBC 기반 비동기 저장/조회 |
| Cache | Redis 기반 외부 API/AI 결과 캐싱 및 토큰 관리 |

---

## 7. WebFlux 적용 설계

| 적용 지점 | 설계 방식 | 어필 포인트 |
|---|---|---|
| 외부 API 병렬 호출 | WebClient + Mono.zip / Flux.merge | 순차 호출 대비 응답 시간 개선 |
| HN item 상세 조회 | Flux.flatMap(concurrency=10) | 많은 네트워크 I/O를 제한된 동시성으로 처리 |
| GitHub 레포별 조회 | Flux.fromIterable + flatMap(concurrency=5) | 관심 레포 여러 개를 병렬 수집 |
| AI 요약 | timeout + fallback summary | AI API 지연/장애가 전체 실패로 번지지 않도록 격리 |
| SSE 진행률 | Flux<ServerSentEvent<ProgressEvent>> | 사용자가 긴 작업 진행 상태를 실시간 확인 |
| DB 저장 | Spring Data R2DBC | DB I/O까지 reactive 흐름 유지 |
| 캐싱 | Reactive Redis | API rate limit 대응 및 재조회 성능 개선 |

### 7.1 브리핑 생성 파이프라인

```java
Mono<GitHubSection> githubMono = githubService.collect(user);
Mono<HackerNewsSection> hnMono = hackerNewsService.collect(user);
Mono<DevToSection> devToMono = devToService.collect(user);

return Mono.zip(githubMono, hnMono, devToMono)
    .flatMap(tuple -> briefingComposer.compose(user, tuple))
    .flatMap(aiSummaryService::summarizeWithFallback)
    .flatMap(briefingRepository::save)
    .doOnNext(saved -> progressService.emitDone(saved));
```

> **확장 기능 예시**: 환율 섹션 추가 시 `Mono<ExchangeSection> exchangeMono = exchangeService.collect()`를 추가하고 `Mono.zip`에 포함하면 됩니다.

### 7.2 Hacker News 상세 조회 예시

```java
hnClient.getTopStoryIds()
    .flatMapMany(Flux::fromIterable)
    .take(50)
    .flatMap(hnClient::getItem, 10)
    .filter(item -> keywordMatcher.matches(item, userKeywords))
    .collectList();
```

---

## 8. 데이터베이스 설계

### 8.1 주요 테이블

| 테이블 | 주요 컬럼 | 설명 |
|---|---|---|
| users | user_id, email, password_hash, created_at | 계정 정보 |
| user_interest | interest_id, user_id, keyword, weight, created_at | 사용자 관심 키워드 |
| watched_repository | repo_id, user_id, owner, repo_name, created_at | 관심 GitHub Repository |
| briefing | briefing_id, user_id, title, summary, status, generated_at | 브리핑 마스터 |

> **briefing.status 확정값** (5단계 구현 전 확정 필요)
>
> | 값 | 의미 |
> |---|---|
> | `GENERATING` | 수집/요약 진행 중 |
> | `COMPLETED` | 모든 소스 수집 + AI 요약 정상 완료 |
> | `PARTIAL` | 일부 외부 API 장애로 부분 수집 완료 |
> | `SUMMARY_FAILED` | 수집은 완료됐으나 AI 요약 실패, 원문 링크는 제공 가능 |
> | `FAILED` | 수집 자체 실패로 브리핑 생성 불가 |
| briefing_item | item_id, briefing_id, source, external_id, title, url, summary, score | 브리핑 개별 항목 |
| api_call_log | log_id, briefing_id, source, status, latency_ms, error_message | 외부 API 호출 로그 |
| saved_article | saved_id, user_id, item_id, memo, created_at | 저장한 글 |
| progress_event | event_id, briefing_id, step, message, created_at | SSE 재연결 대비 진행 이벤트 로그 (아래 결정사항 참고) |

> **progress_event 저장 방식 결정 (6단계 구현 전 확정 필요)**
>
> 아키텍처상 Redis "Progress event buffer"와 DB `progress_event` 테이블이 모두 존재하면 이중 저장이 됨.
>
> | 방식 | 장점 | 단점 |
> |---|---|---|
> | Redis only (TTL 기반) | 구현 단순, DB 부하 없음 | TTL 만료 후 재연결 시 이벤트 복원 불가 |
> | DB persist | 장기 재연결 대응 가능 | DB 저장 비용, 테이블 주기적 정리 필요 |
>
> **MVP 결정**: Redis only로 처리하고 `progress_event` 테이블은 생성하지 않음. 재연결 시 최신 `briefing.status` 조회로 대체. 필요 시 확장 기능으로 DB persist 추가.

### 8.2 인덱스 및 제약

| 테이블 | 인덱스/제약 | 이유 |
|---|---|---|
| users | UNIQUE(email) | 중복 가입 방지 |
| user_interest | UNIQUE(user_id, keyword) | 동일 관심 키워드 중복 방지 |
| watched_repository | UNIQUE(user_id, owner, repo_name) | 동일 레포 중복 등록 방지 |
| briefing | INDEX(user_id, generated_at DESC) | 브리핑 히스토리 조회 성능 |
| briefing_item | INDEX(briefing_id, score DESC) | 브리핑 상세 정렬 |
| api_call_log | INDEX(briefing_id, source) | 출처별 장애/성능 분석 |
| saved_article | UNIQUE(user_id, item_id) | 저장 글 중복 방지 |

---

## 9. API 설계

### 9.1 인증/관심사

| Method | URI | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 및 JWT 발급 |
| POST | `/api/auth/refresh` | Access Token 재발급 |
| POST | `/api/auth/logout` | 로그아웃 및 Refresh Token 제거 |
| GET | `/api/preferences/me` | 내 관심사 조회 |
| POST | `/api/preferences/me/keywords` | 관심 키워드 등록 |
| DELETE | `/api/preferences/me/keywords/{keywordId}` | 관심 키워드 삭제 |
| POST | `/api/preferences/me/repositories` | 관심 GitHub Repository 등록 |
| DELETE | `/api/preferences/me/repositories/{repoId}` | 관심 Repository 삭제 |

### 9.2 브리핑

| Method | URI | 설명 |
|---|---|---|
| POST | `/api/briefings` | 브리핑 생성 요청 |
| GET | `/api/briefings` | 내 브리핑 목록 조회 |
| GET | `/api/briefings/{briefingId}` | 브리핑 상세 조회 |
| POST | `/api/briefings/{briefingId}/stream-token` | SSE 전용 단기 스트림 토큰 발급 (유효기간 3분, 일회성) |
| GET | `/api/briefings/{briefingId}/stream` | SSE 진행률 스트림 (streamToken 쿼리 파라미터 사용) |
| POST | `/api/saved-articles/{itemId}` | 브리핑 항목 저장 |
| GET | `/api/saved-articles` | 저장한 글 목록 조회 |
| DELETE | `/api/saved-articles/{itemId}` | 저장한 글 삭제 |

### 9.3 SSE 이벤트 예시

```text
event: BRIEFING_PROGRESS
data: {
  "briefingId": "BRF-20260519-001",
  "step": "HACKER_NEWS_COLLECTED",
  "processed": 30,
  "total": 100,
  "message": "Hacker News 기술 글 수집 완료"
}

event: BRIEFING_DONE
data: {
  "briefingId": "BRF-20260519-001",
  "message": "오늘의 개발 브리핑 생성 완료"
}
```

---

## 10. 추천/점수화 설계

AI는 브리핑 요약과 추천 이유 생성에 사용하고, 최종 정렬 점수는 서버 내부 규칙 기반으로 계산합니다. 이렇게 하면 추천 결과의 근거를 `keywordMatches`, `sourceWeight`, `recencyScore` 등으로 설명할 수 있습니다.

| 항목 | 점수 반영 방식 |
|---|---|
| 키워드 매칭 | 제목/본문/태그에 사용자 관심 키워드가 포함되는 정도 |
| 출처 가중치 | GitHub 릴리즈, HN 인기글, DEV 글 등 출처별 가중치 |
| 최신성 | 최근 게시글/릴리즈일수록 가산점 |
| 인기도 | GitHub stars, HN score/comments 등 공개 지표 반영 |
| 중복 제거 | 동일 URL/유사 제목은 하나의 대표 항목으로 병합 |

```text
briefingScore =
    keywordMatchScore * 0.40
  + sourceWeightScore * 0.20
  + recencyScore      * 0.20
  + popularityScore   * 0.15
  + diversityScore    * 0.05
```

---

## 11. 인증/인가 및 보안 설계

| 항목 | 설계 |
|---|---|
| JWT 인증 | Access Token 30분, Refresh Token 14일 |
| Refresh Token 저장 (서버) | Redis key: `refresh:{userId}`, TTL은 토큰 만료 시간과 동일 |
| Refresh Token 저장 (클라이언트) | 보안 기준: HttpOnly Secure Cookie 권장. JavaScript 접근을 막아 XSS 피해를 줄임. MVP에서는 구현 편의상 localStorage 사용 가능하나, 포트폴리오 최종 버전에서는 Cookie 기반으로 개선 예정 |
| `/me` 기반 API | Path variable userId를 받지 않고 JWT subject 기준으로 본인 리소스만 접근 |
| 비밀번호 해싱 | BCrypt 사용. CPU 비용이 큰 작업이므로 Netty event-loop 블로킹 방지를 위해 `Schedulers.boundedElastic()`으로 스케줄러 분리 필수. 미적용 시 로그인 요청이 event-loop를 점유해 전체 서버 응답에 영향줌. 구현: `.subscribeOn(Schedulers.boundedElastic())` |
| 외부 API Key | 서버 환경 변수로 관리, 클라이언트에 노출 금지 |
| Rate limit 대응 | GitHub rate limit header, 429 응답 감지, Redis 캐시/백오프 적용 |

---

## 12. 장애 대응 및 성능 전략

| 상황 | 대응 전략 |
|---|---|
| GitHub API rate limit | 인증 요청 사용, 남은 quota 헤더 로깅, Redis 캐싱, 재시도 제한 |
| HN API item 조회 지연 | flatMap concurrency 제한, timeout 후 해당 item 제외 |
| DEV.to API 장애 | 해당 섹션만 비활성화하고 나머지 섹션으로 브리핑 생성 |
| AI API 지연/장애 | timeout 후 다음 순서로 fallback 처리: ① 각 항목 title + source + url만으로 브리핑 구성 → ② 본문이 있는 경우 앞 300자 기준 rule-based summary 생성 → ③ AI 요약 상태를 `SUMMARY_FAILED`로 저장. 사용자는 원문 링크와 기본 메타데이터는 항상 확인 가능. AI 장애가 전체 브리핑 실패로 이어지지 않음 |
| 외부 API 부분 실패 | 전체 실패가 아닌 partial briefing 생성 |
| 중복 데이터 | source + external_id 또는 canonical_url 기준 중복 제거 |
| 브리핑 생성 중 연결 해제 | 진행 이벤트를 DB/Redis에 남겨 재조회 가능하도록 설계 |
| 동시 생성 요청 | 사용자별 진행 중 브리핑 1건으로 제한. Redis 키 `briefing:inprogress:{userId}`로 분산 락 구현. 브리핑 생성 시작 시 키 SET (TTL=브리핑 최대 예상 시간), 완료/실패 시 DEL. 키가 존재하면 409 Conflict 반환. 5단계(브리핑 파이프라인) 구현 시 함께 적용 필수 |

### 12.1 성능 비교 계획

- 순차 호출 방식 vs WebClient 병렬 호출 방식의 평균 응답 시간 비교
- HN item 50개 상세 조회 시 concurrency 1/5/10/20 비교
- Redis 캐싱 적용 전후 동일 키워드 브리핑 생성 시간 비교
- AI API timeout/fallback 적용 전후 실패율과 사용자 체감 응답 비교

---

## 13. 테스트/검증 계획

| 테스트 유형 | 대상 | 검증 내용 |
|---|---|---|
| 단위 테스트 | KeywordMatcher | 관심 키워드 매칭, 대소문자/동의어 처리 |
| 단위 테스트 | BriefingScorer | 점수 계산, 경계값, 정렬 |
| 단위 테스트 | JwtProvider | 토큰 생성/검증/만료 처리 |
| WebFlux 테스트 | BriefingController | WebTestClient로 reactive API 응답 검증 |
| SSE 테스트 | ProgressController | 진행 이벤트 스트림 수신 검증 |
| 외부 API 테스트 | WebClient Adapter | MockWebServer/WireMock으로 timeout/500/429/fallback 검증 |
| Repository 테스트 | R2DBC Repository | 저장/조회/UNIQUE 제약/페이징 검증 |
| 통합 테스트 | 브리핑 생성 파이프라인 | 수집 -> 필터링 -> AI 요약 -> 저장 전체 흐름 검증 |
| 성능 테스트 | 브리핑 생성 API | 순차/병렬 호출 비교 및 concurrency별 처리 시간 측정 |

---

## 14. 개발 일정

| 주차 | 목표 | 주요 작업 | 산출물 |
|---|---|---|---|
| 1주차 | 프로젝트 기반 구축 | ERD 확정, WebFlux/R2DBC/Security 설정, Docker Compose 구성 | 프로젝트 골격, DB 마이그레이션 |
| 2주차 | 인증/관심사 구현 | 회원가입/로그인, JWT, 관심 키워드/레포 API | 인증 API, 관심사 API |
| 3주차 | 외부 API Adapter 구현 | GitHub/HN/DEV.to WebClient Adapter, timeout/fallback | 외부 API 수집 모듈 |
| 4주차 | 브리핑 파이프라인 구현 | Mono.zip/Flux.flatMap 기반 병렬 수집, 점수화, 저장 | 브리핑 생성 API |
| 5주차 | AI 요약/SSE 구현 | AI 요약, Redis 캐시, SSE 진행률 스트림 | AI 요약 및 진행률 기능 |
| 6주차 | 테스트/성능 검증 | 단위/통합/보안 테스트, 순차 vs 병렬 비교 | 테스트 결과, 성능 지표 |
| 7주차 | 포폴 마무리 | README, API 문서, 실행 가이드, 시연 영상 | 완성 포트폴리오 |

---

## 15. 포트폴리오 어필 포인트

> **면접용 핵심 문장**  
> 오늘의 개발은 개발자가 매일 확인해야 하는 GitHub, Hacker News, 기술 블로그 데이터를 WebFlux 기반으로 병렬 수집하고 AI 요약을 통해 개인화 브리핑을 제공하는 서비스입니다. 외부 API 호출이 많은 구조에서 순차 호출 방식의 응답 지연을 해결하기 위해 WebClient, Mono.zip, Flux.flatMap을 적용했으며, timeout, fallback, concurrency 제한, Redis 캐싱을 통해 외부 API 장애와 rate limit에 대응했습니다.

| 어필 항목 | 설명 |
|---|---|
| WebFlux 적용 이유 | 외부 API와 AI API 호출이 많아 I/O 대기 중심의 논블로킹 구조가 자연스럽게 필요 |
| 기술적 깊이 | Mono/Flux, flatMap concurrency, Mono.zip, SSE, R2DBC, Redis, JWT |
| 실무 설계력 | 외부 API 장애 격리, 부분 성공 응답, timeout/fallback, rate limit 대응 |
| 서비스성 | 개발자가 실제로 매일 사용할 수 있는 개인화 기술 브리핑 서비스 |
| 차별화 | 흔한 쇼핑몰/게시판이 아니라 API Aggregation과 AI 요약이 결합된 백엔드 포트폴리오 |
| 확장성 | 자동 브리핑, Slack/Email 전송, MSA 분리, 트렌드 분석 기능으로 확장 가능 |

---

## 16. 프론트엔드 설계

### 16.1 기술 스택

| 항목 | 선택 | 이유 |
|---|---|---|
| 번들러 | Vite | 빠른 개발 서버, HMR |
| UI 라이브러리 | React 18 | SSE, 상태 관리에 적합 |
| 스타일 | Tailwind CSS | 빠른 UI 구성 |
| 서버 상태 관리 | React Query (TanStack Query) | API 호출/캐싱/재시도 처리 |
| HTTP 클라이언트 | Axios | JWT 인터셉터 구성 편의 |
| 라우팅 | React Router v6 | SPA 라우팅 |
| SSE | 브라우저 내장 `EventSource` API | 별도 라이브러리 불필요 |

### 16.2 화면 구성

| 화면 | 경로 | 핵심 UI 요소 |
|---|---|---|
| 로그인 | `/login` | 이메일/비밀번호 폼, JWT 저장 |
| 회원가입 | `/signup` | 이메일/비밀번호 입력 |
| 관심사 설정 | `/preferences` | 관심 키워드 태그 등록/삭제, 관심 레포 등록/삭제 |
| 브리핑 홈 | `/` | 오늘의 브리핑 카드, 생성 버튼, 섹션별 탭 |
| 브리핑 생성 중 | `/briefings/:briefingId/loading` | SSE 진행률 바 실시간 표시 |
| 브리핑 상세 | `/briefings/:briefingId` | GitHub/HN/DEV 섹션별 아티클 카드, AI 요약, 저장 버튼 |
| 브리핑 히스토리 | `/briefings` | 과거 브리핑 목록, 날짜별 조회 |
| 저장한 글 | `/saved` | 저장 아티클 목록, 메모 수정/삭제 |

### 16.3 JWT 관리 전략

| 항목 | 저장 위치 | 이유 |
|---|---|---|
| Access Token | 메모리 또는 `sessionStorage` | 짧은 유효기간, API 요청 시 Authorization 헤더 첨부 |
| Refresh Token | HttpOnly Secure Cookie 권장 | JavaScript 접근 차단, XSS 피해 감소. MVP에서는 구현 편의상 `localStorage` 허용하되 최종 버전에서 Cookie 방식으로 개선 예정 |

```js
// Axios 인터셉터 — 모든 요청에 Access Token 자동 첨부
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 응답 시 Cookie의 Refresh Token으로 자동 재발급 (withCredentials로 쿠키 자동 전송)
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      const { data } = await api.post('/api/auth/refresh', null, {
        withCredentials: true
      });
      sessionStorage.setItem('accessToken', data.accessToken);
      error.config.headers.Authorization = `Bearer ${data.accessToken}`;
      return api(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 16.4 SSE 연동 설계

`EventSource`는 커스텀 Authorization 헤더를 지원하지 않습니다. 일반 Access Token을 URL에 그대로 노출하면 서버 로그와 브라우저 히스토리에 토큰이 남을 수 있으므로, SSE 연결 전 **단기 스트림 토큰**을 별도 발급해 사용합니다.

```js
async function useBriefingStream(briefingId, onProgress, onDone) {
  useEffect(() => {
    let es;

    // 1. SSE 전용 단기 토큰 발급 (유효기간 3분, 일회성)
    api.post(`/api/briefings/${briefingId}/stream-token`)
      .then(({ data }) => {
        // 2. 발급받은 스트림 토큰으로 SSE 연결
        es = new EventSource(
          `/api/briefings/${briefingId}/stream?streamToken=${data.streamToken}`
        );

        es.addEventListener('BRIEFING_PROGRESS', (e) => {
          onProgress(JSON.parse(e.data));
        });

        es.addEventListener('BRIEFING_DONE', (e) => {
          onDone(JSON.parse(e.data));
          es.close();
        });

        es.onerror = () => es.close();
      });

    return () => es?.close();
  }, [briefingId]);
}
```

> **백엔드 대응**: `/api/briefings/{briefingId}/stream-token` API로 3분짜리 일회성 스트림 토큰을 발급하고, SSE 필터에서 해당 토큰만 검증합니다. 일반 Access Token은 URL에 노출되지 않습니다.

### 16.5 핵심 화면 상세: 브리핑 생성 진행 화면

```text
[브리핑 생성 중]

  ✅ GitHub 릴리즈/이슈 수집 완료        12건
  ✅ Hacker News 기술 글 수집 완료       30건
  ⏳ DEV.to 태그 글 수집 중...
  ░░░░░░░░░░░░░░░░░░░░░░░  0%

  전체 진행률
  ████████████░░░░░░░░░░  55%
  AI 요약 대기 중...

[완료 후 → 브리핑 상세 페이지로 자동 이동]
```

### 16.6 핵심 화면 상세: 브리핑 상세 화면

```text
[오늘의 개발 브리핑 — 2026.05.19]

AI 한 줄 요약: "오늘은 Spring Boot 3.4 릴리즈와 Rust 관련 글이 핫합니다."

[GitHub]  [Hacker News]  [DEV.to]   ← 섹션 탭

┌─────────────────────────────────────┐
│ spring-projects/spring-boot v3.4.0  │
│ ⭐ 관련도 95점                        │
│ AI 요약: Virtual Thread 지원 강화,   │
│ R2DBC 자동 설정 개선 포함            │
│ [원문 보기]  [저장]                   │
└─────────────────────────────────────┘
```

### 16.7 프론트엔드 디렉토리 구조

```text
src/
├─ api/
│  ├─ axios.ts               (인터셉터 설정)
│  ├─ auth.ts
│  ├─ briefings.ts
│  ├─ preferences.ts
│  └─ saved.ts
├─ hooks/
│  └─ useBriefingStream.ts
├─ pages/
│  ├─ LoginPage.tsx
│  ├─ SignupPage.tsx
│  ├─ PreferencesPage.tsx
│  ├─ BriefingHomePage.tsx
│  ├─ BriefingLoadingPage.tsx
│  ├─ BriefingDetailPage.tsx
│  ├─ BriefingHistoryPage.tsx
│  └─ SavedArticlesPage.tsx
├─ components/
│  ├─ ProgressBar.tsx
│  ├─ BriefingCard.tsx
│  ├─ ArticleCard.tsx
│  ├─ SectionTabs.tsx
│  └─ KeywordTag.tsx
├─ types/
│  ├─ briefing.ts            (BriefingStatus, BriefingItem, source enum 등)
│  └─ sse.ts                 (ProgressEvent, BriefingDoneEvent 타입)
└─ App.tsx
```

### 16.8 개발 일정 (프론트)

백엔드 API가 완성된 순서에 맞춰 붙입니다.

| 주차 | 작업 |
|---|---|
| 3주차 | 프로젝트 셋업, 로그인/회원가입, Axios 인터셉터, 라우팅 구성 |
| 4주차 | 관심사 설정(키워드/레포 관리), 브리핑 홈 화면 |
| 5주차 | 브리핑 생성 + SSE 진행률 화면, 브리핑 상세 화면 |
| 6주차 | 브리핑 히스토리, 저장한 글, 전체 연동 QA |

---

## 17. 추천 패키지 구조

```text
com.todaydev
├─ auth
│  ├─ web
│  ├─ service
│  ├─ domain
│  ├─ repository
│  └─ filter
├─ preference
│  ├─ web
│  ├─ service
│  ├─ domain
│  └─ repository
├─ briefing
│  ├─ web
│  ├─ service
│  ├─ domain
│  └─ repository
├─ external
│  ├─ github
│  ├─ hackernews
│  └─ devto
│  (확장 시 exchange 패키지 추가)
├─ ai
│  ├─ client
│  └─ service
├─ progress
│  ├─ web
│  └─ service
└─ common
   ├─ config
   ├─ exception
   └─ response
```

---

## 18. README 구성안

- 프로젝트 소개: 오늘의 개발이 해결하는 문제와 핵심 기능
- 기술 스택: WebFlux, WebClient, R2DBC, PostgreSQL, Redis, SSE, JWT, AI API
- 아키텍처 그림: Client - API Server - External APIs - DB/Redis
- 핵심 구현: Reactive 브리핑 생성 파이프라인, timeout/fallback, SSE 진행률, JWT 인증
- 성능 비교: 순차 호출 vs 병렬 호출, Redis 캐시 전후 응답 시간
- 트러블슈팅: rate limit 대응, AI timeout, SSE 재연결, R2DBC UPSERT
- 실행 방법: Docker Compose, 환경 변수 목록, API Key 설정, 샘플 요청

---

## 19. 참고 API 문서

아래 문서는 외부 API 사용 가능성 및 설계 판단의 근거로 사용합니다. 실제 구현 시점에는 각 공식 문서의 최신 rate limit과 이용 약관을 다시 확인합니다.

- GitHub REST API Rate Limits: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- Hacker News Official API: https://github.com/HackerNews/API
- Forem/DEV API Docs: https://developers.forem.com/api/v0
- Frankfurter Exchange Rates API: https://frankfurter.dev/

---

## 최종 결론

**오늘의 개발**은 추상적인 API Aggregation Dashboard가 아니라, 개발자가 실제로 매일 사용할 수 있는 맞춤형 기술 브리핑 서비스입니다. GitHub, Hacker News, DEV/블로그, AI 요약 API를 병렬 호출하는 구조라 WebFlux의 장점이 자연스럽게 드러나며, 외부 API rate limit과 장애를 고려한 설계까지 포트폴리오에서 설명할 수 있습니다.

MVP 기준으로는 브리핑 생성, 관심사 관리, AI 요약, SSE 진행률, Redis 캐싱, R2DBC 저장을 완성하고, 이후 자동 발송과 트렌드 분석으로 확장하는 방식이 가장 안정적입니다.
