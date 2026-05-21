# Backend Development Plan

이 문서는 `오늘의 개발` 백엔드를 어떤 순서로 구현할지 정리한 개발 계획입니다. AI 또는 사람이 개발할 때 이 순서를 기준으로 진행하며, 앞 단계의 계약/테스트/보안 기준이 무너지지 않도록 합니다.

## 0. 개발 전 확인

구현 전 반드시 확인할 문서:

- `API_CONTRACT.md`
- `CODING_RULES.md`
- `TESTING_RULES.md`
- `DONE_DEFINITION.md`

원칙:

- API 계약을 먼저 보고 DTO와 ErrorCode를 만든다.
- 기능 구현과 구조 리팩터링을 섞지 않는다.
- 인증, 공통 응답, 에러 처리 없이 도메인 API부터 만들지 않는다.
- 외부 API 연동 전에 timeout/fallback 정책을 먼저 정한다.

## 1. 공통 기반

목표:

- 모든 API가 같은 응답/에러 포맷을 사용한다.
- 보안성 검사에 걸릴 만한 raw error 노출을 초기에 차단한다.

작업:

- 공통 성공 응답 DTO
- 공통 에러 응답 DTO
- ErrorCode enum
- GlobalExceptionHandler
- Validation error 변환
- traceId/correlation id 정책
- 공통 시간 포맷 정책

완료 기준:

- `API_CONTRACT.md`의 공통 응답 포맷과 일치한다.
- raw exception message, stack trace, SQL, token이 응답에 노출되지 않는다.

## 2. 설정과 보안 기반

목표:

- secret, timeout, TTL, external base URL을 하드코딩하지 않는다.

작업:

- `@ConfigurationProperties` 기반 설정 분리
- JWT 설정
- Redis key prefix/TTL 설정
- External API base URL 설정
- CORS 설정
- SecurityWebFilterChain 기본 구성

완료 기준:

- `@Value`가 여러 클래스에 흩어지지 않는다.
- secret은 환경 변수로 주입된다.
- CORS `*`가 기본값이 아니다.

## 3. 인증

목표:

- 회원가입, 로그인, 토큰 재발급, 로그아웃 기반을 만든다.

작업:

- User domain/entity
- R2DBC UserRepository
- PasswordEncoder
- BCrypt boundedElastic 격리
- JwtProvider
- Refresh token Redis 저장
- AuthController/AuthService

완료 기준:

- 비밀번호 hash는 응답에 포함되지 않는다.
- Access token과 refresh token 로그가 남지 않는다.
- `/me` 계열 API를 위한 인증 사용자 식별 방식이 준비된다.

## 4. 관심사와 Repository 관리

목표:

- 사용자가 관심 키워드와 GitHub repository를 관리할 수 있다.

작업:

- InterestKeyword domain
- WatchedRepository domain
- 중복 등록 방지
- owner/repoName validation
- PreferenceController/Service/Repository

완료 기준:

- 클라이언트가 넘긴 userId를 신뢰하지 않는다.
- JWT subject 기준으로 본인 리소스만 접근한다.
- 중복 등록은 `409`와 계약된 ErrorCode로 응답한다.

## 5. 외부 API Adapter

목표:

- GitHub, Hacker News, DEV.to 호출을 WebClient adapter로 분리한다.

작업:

- GitHub client
- Hacker News client
- DEV.to client
- WebClient 공통 설정
- timeout
- retry 제한
- rate limit 감지
- 외부 응답 DTO와 내부 모델 변환

완료 기준:

- 외부 API 응답 DTO가 도메인 모델에 직접 섞이지 않는다.
- timeout/fallback 없이 외부 API를 호출하지 않는다.
- source별 장애가 전체 장애로 번지지 않는다.

## 6. 브리핑 도메인과 점수화

목표:

- 수집된 항목을 관심사 기준으로 필터링하고 점수화한다.

작업:

- Briefing domain
- BriefingItem domain
- BriefingStatus enum
- Source enum
- KeywordMatcher
- BriefingScorer
- 중복 제거 정책

완료 기준:

- 점수화 규칙이 테스트 가능하다.
- status/source 문자열을 하드코딩하지 않는다.
- AI 없이도 기본 브리핑 항목을 구성할 수 있다.

## 7. 브리핑 생성 파이프라인

목표:

- WebFlux 기반 병렬 수집 파이프라인을 만든다.

작업:

- `Mono.zip` 기반 source 병렬 수집
- `Flux.flatMap(..., concurrency)` 적용
- partial success 처리
- Redis in-progress lock
- api_call_log 저장
- briefing/briefing_item 저장

완료 기준:

- 사용자는 동시에 진행 중인 브리핑을 1개만 만들 수 있다.
- 외부 API 일부 실패 시 `PARTIAL` 상태가 가능하다.
- `.block()` 또는 직접 `.subscribe()`가 없다.

## 8. AI 요약

목표:

- AI 요약을 브리핑에 붙이되, AI 실패가 전체 실패가 되지 않게 한다.

작업:

- AI client
- AI summary service
- timeout
- fallback summary
- summary failed 상태 처리
- AI 결과 Redis cache

완료 기준:

- AI timeout 시 `SUMMARY_FAILED` 또는 fallback 정책이 적용된다.
- AI API key는 서버 환경 변수로만 관리된다.
- 프론트에 AI 내부 오류가 노출되지 않는다.

## 9. SSE 진행률

목표:

- 긴 브리핑 생성 과정을 실시간으로 전달한다.

작업:

- stream token 발급
- stream token Redis 저장/검증
- progress event buffer
- SSE endpoint
- `BRIEFING_PROGRESS`, `BRIEFING_DONE`, `BRIEFING_PARTIAL_DONE`, `BRIEFING_FAILED`

완료 기준:

- 일반 access token을 SSE query string에 넣지 않는다.
- stream token은 짧은 TTL을 가진다.
- 종료 이벤트 이후 프론트가 연결을 닫을 수 있다.

## 10. 저장한 글

목표:

- 브리핑 항목을 저장하고 메모를 관리한다.

작업:

- saved article 저장
- 중복 저장 방지
- 저장 목록 조회
- 메모 수정/삭제가 필요하면 별도 API로 분리

완료 기준:

- 본인 글만 접근 가능하다.
- 같은 item 중복 저장은 막는다.

## 11. 성능/검증/문서

목표:

- 포트폴리오에서 WebFlux 선택 이유를 설명 가능한 자료를 만든다.

작업:

- 순차 호출 vs 병렬 호출 비교
- HN concurrency 비교
- Redis cache 전후 비교
- README 실행 방법 정리
- API 예시 정리

완료 기준:

- 테스트가 통과한다.
- 성능 비교 기준이 문서화된다.
- 실행 방법과 환경 변수가 문서화된다.

