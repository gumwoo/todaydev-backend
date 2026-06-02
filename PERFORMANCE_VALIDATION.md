# Backend Performance And Validation

이 문서는 11단계 기준으로 Todaydev 백엔드의 성능 비교 기준, 검증 절차, 포트폴리오 설명 포인트를 정리합니다.

## 목적

- WebFlux를 선택한 이유를 설명 가능하게 만든다.
- 외부 API 병렬 수집 구조의 장점을 수치로 비교할 수 있게 만든다.
- Redis cache, timeout, fallback, SSE를 어떤 기준으로 검증할지 정한다.
- 운영 전 반복 가능한 검증 절차를 남긴다.

## 현재 성능 설계 요약

브리핑 생성은 아래 흐름으로 동작합니다.

```text
POST /api/briefings
-> Redis in-progress lock
-> briefing row 생성 후 GENERATING 즉시 반환
-> background dispatcher
-> GitHub / Hacker News / DEV.to 병렬 수집
-> 후보 생성 / 점수화 / 중복 제거
-> AI summary
-> DB 저장
-> Redis progress buffer + SSE terminal event
```

핵심 설계:

- HTTP 요청 thread가 외부 API 전체 완료를 기다리지 않는다.
- 외부 source 수집은 `Flux.merge`로 병렬화한다.
- Hacker News item fetch는 concurrency 제한을 둔다.
- 후보 생성은 `candidate-concurrency` 설정으로 제한한다.
- AI 실패는 브리핑 저장 전체 실패로 전파하지 않고 fallback summary와 `SUMMARY_FAILED`로 기록한다.
- 진행률은 Redis buffer에 보관하여 SSE 연결 시점이 늦어도 재생 가능하다.

## WebFlux 선택 이유

이 서비스는 CPU 연산보다 외부 API와 DB/Redis I/O 대기가 많습니다.

WebFlux가 맞는 이유:

- GitHub, Hacker News, DEV.to, Gemini 호출처럼 대기 시간이 큰 작업이 많다.
- 외부 API 일부 실패를 fallback으로 격리하기 쉽다.
- `Mono`/`Flux` chain으로 timeout, retry, partial success를 명시적으로 관리할 수 있다.
- SSE 진행률 전달과 비동기 브리핑 생성 흐름이 자연스럽다.

주의할 점:

- business service/controller에서 직접 `.subscribe()`하지 않는다.
- background job 소비에 필요한 subscription boundary는 `infrastructure` 단일 위치에만 둔다.
- blocking 작업은 `boundedElastic`로 격리한다.
- timeout과 concurrency는 설정으로 중앙 관리한다.

## 측정 대상

### 1. 순차 호출 vs 병렬 호출

목적:

- 외부 source 수집을 순차 처리했을 때와 병렬 처리했을 때의 브리핑 생성 완료 시간을 비교한다.

측정 항목:

- `POST /api/briefings` 접수 응답 시간
- 최종 DB status가 `COMPLETED`, `PARTIAL`, `SUMMARY_FAILED`, `FAILED`가 되기까지 걸린 시간
- `api_call_log.latency_ms` source별 latency
- 저장된 `briefing_item` 개수
- terminal SSE event 도착 시간

현재 병렬 기준:

- `ExternalArticleCollector.collect`에서 GitHub, Hacker News, DEV.to를 `Flux.merge`로 수집한다.

순차 비교 기준:

- 비교용 branch 또는 실험 코드에서 source를 `concat` 방식으로 순차 수집한다.
- 기능 코드는 benchmark 목적 외에는 순차화하지 않는다.

권장 결과 표:

```text
scenario, source_count, item_count, accepted_ms, terminal_ms, github_ms, hacker_news_ms, devto_ms, ai_ms, final_status
parallel, 3, 30, 30, 8200, 1200, 1800, 1400, 5000, COMPLETED
sequential, 3, 30, 30, 11200, 1200, 1800, 1400, 5000, COMPLETED
```

판단 기준:

- 병렬 수집의 terminal time이 source latency 합보다 source latency max에 가깝게 줄어드는지 확인한다.
- accepted time은 생성 완료 시간과 분리되어 짧게 유지되어야 한다.

### 2. Hacker News concurrency 비교

목적:

- Hacker News top story item fetch 동시성 값을 변경했을 때 latency와 안정성을 비교한다.

현재 기준:

- Hacker News item fetch는 client 내부 concurrency 제한을 둔다.
- story 수는 `briefing.collection.hacker-news-story-limit`로 관리한다.

측정 변수:

- story limit: `10`, `20`, `30`
- item concurrency: 실험 branch에서 `3`, `5`, `10` 비교

측정 항목:

- Hacker News `api_call_log.latency_ms`
- 실패율
- 전체 terminal time
- 외부 API timeout 발생 여부

권장 판단:

- concurrency를 무조건 높이지 않는다.
- timeout이나 rate limit이 늘면 성능 개선으로 보지 않는다.
- 안정적인 p95 latency가 더 중요하다.

### 3. Redis cache 전후 비교

현재 상태:

- Redis는 refresh token, briefing lock, progress buffer, stream token에 사용 중이다.
- `api-cache`, `ai-summary` key prefix와 TTL 설정은 준비되어 있다.
- 외부 API 응답 cache와 AI summary cache는 아직 실제 기능으로 붙이지 않았다.

비교 기준:

- cache 도입 전 baseline을 먼저 기록한다.
- cache 도입 후 동일 관심사/동일 repository 조건으로 반복 생성 시간을 비교한다.

측정 항목:

- 외부 API source별 latency
- AI latency
- terminal time
- cache hit/miss count
- Redis memory usage

권장 결과 표:

```text
scenario, cache_hit, terminal_ms, github_ms, hacker_news_ms, devto_ms, ai_ms, final_status
before_cache, false, 8200, 1200, 1800, 1400, 5000, COMPLETED
after_cache, true, 2600, 50, 80, 70, 2400, COMPLETED
```

cache 도입 규칙:

- token, API key, Authorization header를 cache key/value에 넣지 않는다.
- 사용자별 민감 데이터가 섞일 수 있는 값은 공용 cache에 저장하지 않는다.
- TTL은 `application.yml`의 `app.redis.keys`에서 중앙 관리한다.
- cache 실패가 브리핑 생성 실패로 번지지 않게 한다.

## 실행 검증 절차

### 1. 인프라 확인

```powershell
docker compose up -d
docker compose ps
docker exec todaydev-postgres psql -U todaydev -d todaydev -c "\dt"
docker exec todaydev-redis redis-cli ping
```

### 2. 테스트 실행

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
```

통과 기준:

- 전체 테스트 `BUILD SUCCESSFUL`
- 테스트가 실제 외부 API key나 네트워크에 의존하지 않음

### 3. WebFlux 규칙 확인

```powershell
rg -n "\.block\(|\.blockOptional\(|\.subscribe\(" src/main/java src/test/java
```

통과 기준:

- `.block()` 없음
- `.subscribe()`는 `briefing/infrastructure/BriefingJobDispatcher.java`의 단일 subscription boundary만 허용

### 4. API 계약 인코딩 확인

```powershell
$backend=Get-Content -Raw -Encoding UTF8 "API_CONTRACT.md"
$frontend=Get-Content -Raw -Encoding UTF8 "..\todaydev-frontend\API_CONTRACT.md"
$backend -eq $frontend
```

통과 기준:

- backend/frontend API 계약이 동일함
- 한글 깨짐 marker가 없음

### 5. 브리핑 생성 E2E 기준

검증 흐름:

- 회원가입
- 로그인
- 관심 키워드 등록
- 관심 repository 등록
- `POST /api/briefings`
- stream token 발급
- SSE 수신
- DB status와 `api_call_log` 확인

통과 기준:

- 접수 응답은 `202`와 `GENERATING`
- stream token 재사용은 `401 STREAM_TOKEN_EXPIRED`
- 성공 시 `COMPLETED`
- AI 실패 시 `SUMMARY_FAILED`와 fallback summary
- source 실패 일부는 `PARTIAL` 또는 `SUMMARY_FAILED`로 격리

### 6. 저장한 글 E2E 기준

검증 흐름:

- 본인 briefing item 저장
- 저장 목록 조회
- 메모 수정
- 삭제
- 중복 저장
- 다른 사용자 item 저장 시도

통과 기준:

- 저장 성공 `201`
- 목록 조회 `200`
- 메모 수정 `200`
- 삭제 `200`
- 중복 저장 `409 SAVED_ARTICLE_DUPLICATED`
- 권한 밖 item 저장 `404 SAVED_ARTICLE_NOT_FOUND`
- 삭제 후 수정 `404 SAVED_ARTICLE_NOT_FOUND`

## API 예시 위치

대표 API one-liner는 `README.md`에 정리한다.

상세 계약은 `API_CONTRACT.md`를 기준으로 한다.

## 아직 남은 검증

- 프론트엔드 실제 브라우저 연동
- 운영 cookie `secure(true)` 검증
- 운영 CORS origin 검증
- 외부 API cache 실제 도입 후 before/after 수치 기록
- 부하 테스트 도구를 이용한 p95/p99 latency 측정

## 포트폴리오 설명 포인트

- 요청 접수와 실제 브리핑 생성을 분리해 UX 대기 시간을 줄였다.
- 외부 API는 병렬 수집하고, 실패 source는 전체 실패로 전파하지 않는다.
- SSE와 Redis progress buffer로 진행률을 안정적으로 전달한다.
- secret은 URL/query/log/응답에 노출하지 않고 환경 변수 또는 ignored local config로 관리한다.
- 저장한 글은 DB 조인과 userId 조건으로 권한 경계를 보장한다.
