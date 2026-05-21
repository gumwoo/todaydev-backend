# Backend Definition of Done

이 문서는 백엔드 작업을 "완료"라고 말하기 전 반드시 확인해야 하는 기준입니다.

## 1. 공통 완료 기준

- `API_CONTRACT.md`와 응답 구조가 일치한다.
- `CODING_RULES.md`를 위반하지 않는다.
- `TESTING_RULES.md` 기준의 필요한 테스트가 작성되었거나, 작성하지 못한 이유가 명확하다.
- 하드코딩된 secret, token, URL, TTL, timeout, Redis key가 없다.
- 새 enum/status/error code가 문서화되어 있다.
- 기능 구현과 리팩터링이 불필요하게 섞이지 않았다.

## 2. API 완료 기준

- HTTP status가 계약과 일치한다.
- 성공 응답은 `success: true`, `data`, `timestamp`를 포함한다.
- 실패 응답은 `success: false`, `error.code`, `error.message`, `error.details`, `error.traceId`, `timestamp`를 포함한다.
- Validation error가 공통 포맷으로 내려간다.
- Entity를 그대로 응답하지 않는다.
- passwordHash, token, API key, stack trace가 노출되지 않는다.

## 3. 보안 완료 기준

- 인증이 필요한 API는 인증 없이 접근할 수 없다.
- 본인 리소스만 접근 가능하다.
- 클라이언트가 넘긴 userId를 신뢰하지 않는다.
- CORS 설정이 무분별하지 않다.
- raw exception message를 사용자에게 반환하지 않는다.
- `printStackTrace`, `System.out.println`이 없다.
- secret은 환경 변수 또는 안전한 설정으로 관리된다.

## 4. WebFlux 완료 기준

- 비즈니스 코드에 `.block()`이 없다.
- 비즈니스 코드에 직접 `.subscribe()`가 없다.
- blocking 작업은 boundedElastic로 격리된다.
- 외부 API 호출에는 timeout/fallback이 있다.
- 다량 호출에는 concurrency 제한이 있다.
- partial success가 가능한 흐름은 전체 실패로 처리하지 않는다.

## 5. 테스트 완료 기준

- 단위 테스트가 필요한 핵심 로직에 있다.
- API 테스트가 공통 응답 포맷을 검증한다.
- 실패 케이스 테스트가 있다.
- 외부 API 장애 테스트가 있다.
- 테스트가 외부 네트워크나 실제 API key에 의존하지 않는다.
- 로컬에서 테스트 실행 결과를 확인했다.

## 6. 문서 완료 기준

- API 계약 변경이 있으면 `API_CONTRACT.md`를 갱신했다.
- 설정값 추가가 있으면 환경 변수 문서 또는 README 갱신 대상인지 확인했다.
- 새 ErrorCode, enum, SSE event가 있으면 문서에 반영했다.
- 프론트와 맞닿는 변경은 프론트 계약 문서도 함께 갱신했다.

## 7. AI 작업 완료 체크리스트

AI가 백엔드 작업 후 반드시 확인한다.

- 이 변경은 어느 단계의 개발 계획에 속하는가?
- API 계약을 깨지 않았는가?
- 보안 스캐너에 걸릴 만한 코드가 없는가?
- 테스트 가능한 구조인가?
- Controller가 비대해지지 않았는가?
- 설정값이 중앙 관리되는가?
- 실패 시나리오가 고려되었는가?
- 프론트가 응답을 예측 가능하게 받을 수 있는가?

