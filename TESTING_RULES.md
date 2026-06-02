# Backend Testing Rules

이 문서는 백엔드 테스트 기준입니다. 기능이 동작하는 것뿐 아니라 API 계약, 보안, WebFlux 흐름, 장애 대응이 지켜지는지 검증합니다.

## 1. 테스트 원칙

- 새 기능은 성공 케이스와 실패 케이스를 함께 테스트한다.
- API 계약과 다른 응답은 테스트 실패로 본다.
- 보안상 민감정보가 응답/로그에 노출될 수 있는 코드는 테스트 또는 리뷰에서 막는다.
- WebFlux 코드에서 `.block()`으로 테스트를 쉽게 만들려고 하지 않는다.
- 외부 API는 실제 네트워크 대신 mock server를 우선 사용한다.

## 2. 테스트 범위

공통:

- 공통 성공 응답 포맷
- 공통 에러 응답 포맷
- Validation error 응답
- ErrorCode 매핑
- traceId 포함 여부

인증:

- 회원가입 성공
- 중복 이메일 실패
- 로그인 성공
- 잘못된 비밀번호 실패
- JWT 생성/검증/만료
- refresh token 검증
- 비밀번호 hash 검증
- BCrypt boundedElastic 분리 여부

관심사:

- 키워드 등록/삭제/조회
- 키워드 중복 등록 실패
- repository 등록/삭제/조회
- owner/repoName validation
- 다른 사용자 리소스 접근 차단

외부 API:

- GitHub 정상 응답
- GitHub 403/rate limit
- Hacker News item timeout
- DEV.to 장애
- 500/429/timeout fallback
- 외부 응답 DTO와 내부 모델 변환

브리핑:

- KeywordMatcher
- BriefingScorer
- 중복 제거
- status 전이
- partial success
- in-progress lock
- briefing 저장/조회

저장한 글:

- 저장 성공
- 중복 저장 실패
- 본인 브리핑 아이템만 저장 가능
- 저장 목록 paging
- 메모 수정
- 삭제
- 다른 사용자 저장 글 접근 차단

AI:

- AI 요약 성공
- AI timeout
- AI rate limit
- fallback summary
- `SUMMARY_FAILED` 상태 처리

SSE:

- stream token 발급
- stream token 만료
- invalid token 거부
- progress event 전송
- done/partial/failed 종료 이벤트

Repository:

- R2DBC 저장/조회
- unique 제약
- cascade/delete 정책
- paging

## 3. 도구 기준

- Controller/API 테스트: `WebTestClient`
- Reactor 흐름 테스트: `reactor-test`
- 외부 API 테스트: MockWebServer 또는 WireMock
- Repository 테스트: 실제 PostgreSQL과 가까운 환경을 우선한다.
- 단위 테스트: JUnit 5
- 보안 테스트: Spring Security Test

## 4. API 계약 테스트 기준

모든 API 테스트는 아래를 확인한다.

- `success` 값이 올바른가?
- 성공 시 `data`가 있는가?
- 실패 시 `error.code`, `error.message`, `error.details`, `error.traceId`가 있는가?
- timestamp가 ISO-8601 형식인가?
- HTTP status와 ErrorCode가 계약과 일치하는가?
- Entity 내부 필드가 그대로 노출되지 않는가?

## 5. 보안 테스트 기준

응답에 절대 포함되면 안 되는 값:

- password
- passwordHash
- accessToken을 제외한 내부 token 원문
- refresh token
- stream token 검증 정보
- API key
- Authorization header
- stack trace
- exception class name
- SQL
- 내부 파일 경로

로그/응답 검토 대상:

- raw exception message
- `printStackTrace`
- `System.out.println`
- request/response 전체 dump

## 6. WebFlux 테스트 기준

- Service/Controller에서 `.block()`을 사용하지 않는다.
- 직접 `.subscribe()`로 side effect를 만들지 않는다.
- blocking 작업은 boundedElastic로 격리한다.
- timeout/fallback이 테스트된다.
- concurrency 제한이 필요한 곳은 값이 설정으로 관리된다.

## 7. 완료 전 필수 테스트

기능별 최소 테스트:

- Auth 기능: 인증 성공/실패/토큰 만료
- Preference 기능: CRUD/중복/인가
- External Adapter: 정상/timeout/500/429
- Briefing Pipeline: completed/partial/failed
- AI Summary: success/fallback/failed
- SSE: progress/done/invalid token

## 8. 테스트 이름 규칙

테스트 이름은 상황과 기대 결과가 드러나야 한다.

예:

```text
login_returnsAccessToken_whenCredentialsAreValid
createBriefing_returnsConflict_whenBriefingAlreadyInProgress
githubClient_returnsFallback_whenGithubTimesOut
```

## 9. 테스트 금지 사항

- 실제 API key가 필요한 테스트 금지
- 테스트가 외부 네트워크에 의존하는 것 금지
- 테스트 순서에 의존하는 것 금지
- 과도한 sleep으로 비동기 테스트를 처리하는 것 금지
- 보안 설정을 꺼놓고 통과시키는 것 금지
