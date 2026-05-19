# Backend Coding Rules

이 문서는 AI 또는 사람이 백엔드 코드를 작성할 때 반드시 지켜야 하는 기준입니다. 목표는 기능 구현 속도보다 **유지보수 가능한 모듈 분리**, **중앙 설정 관리**, **하드코딩 방지**, **WebFlux 흐름 보존**을 우선하는 것입니다.

## 1. 핵심 원칙

- 코드는 "일단 동작"보다 "나중에 설명 가능하고 변경 가능한 구조"를 우선한다.
- 하드코딩된 URL, 토큰, 만료 시간, 동시성 수, Redis key prefix, 에러 메시지는 만들지 않는다.
- 설정값은 `application.yml`, `@ConfigurationProperties`, enum, constants 클래스로 중앙 관리한다.
- Controller는 얇게 유지하고 비즈니스 로직은 Service/Application 계층에 둔다.
- 외부 API 호출, AI 호출, Redis, DB 접근은 각각 전용 모듈/어댑터로 분리한다.
- WebFlux 프로젝트이므로 reactive 흐름을 임의로 끊지 않는다.

## 2. 패키지 구조 규칙

권장 기본 구조:

```text
com.todaydev
├─ auth
├─ preference
├─ briefing
├─ external
│  ├─ github
│  ├─ hackernews
│  └─ devto
├─ ai
├─ progress
└─ common
   ├─ config
   ├─ exception
   ├─ response
   └─ security
```

패키지 규칙:

- `auth`: 회원가입, 로그인, JWT, refresh token 관련 코드만 둔다.
- `preference`: 관심 키워드, 관심 GitHub repository 관리만 둔다.
- `briefing`: 브리핑 생성, 조회, 점수화, 저장 관련 도메인 로직을 둔다.
- `external`: 외부 API 호출 전용 어댑터를 둔다.
- `ai`: Gemini/OpenAI 등 AI 요약 호출과 fallback 로직을 둔다.
- `progress`: SSE 진행률 이벤트와 stream token 관리를 둔다.
- `common`: 공통 설정, 예외, 응답 포맷, 유틸성 코드를 둔다.
- 다른 도메인의 Repository나 Entity를 직접 참조해야 한다면 먼저 모듈 경계를 다시 검토한다.

## 3. 계층 분리 규칙

Controller:

- 요청/응답 DTO 변환과 인증 사용자 식별만 담당한다.
- 외부 API 호출, DB 조합, 점수 계산, Redis 접근을 직접 하지 않는다.

Service/Application:

- 유스케이스 흐름을 조합한다.
- 여러 모듈을 조합하는 경우 `BriefingApplicationService`처럼 목적이 드러나는 이름을 사용한다.

Domain:

- 점수 계산, 상태 변경, 검증 같은 핵심 규칙을 둔다.
- Spring Web, DB, Redis, WebClient에 의존하지 않도록 노력한다.

Repository:

- DB 저장/조회만 담당한다.
- 비즈니스 조건 분기는 Service 또는 Domain에서 처리한다.

Client/Adapter:

- GitHub, Hacker News, DEV.to, AI API 호출은 각각 전용 client로 분리한다.
- WebClient 생성과 base URL 설정은 중앙 설정에서 주입받는다.

## 4. 하드코딩 금지 규칙

아래 값은 코드에 직접 쓰지 않는다.

- 외부 API base URL
- API key, token, secret
- JWT 만료 시간
- Redis key prefix
- timeout duration
- retry 횟수
- WebClient concurrency 값
- SSE stream token TTL
- 허용 origin
- 에러 응답 코드/메시지 문자열

관리 위치 예시:

- 환경별 설정: `application.yml`
- 타입 안전 설정: `@ConfigurationProperties`
- 고정 상태값: enum
- 공통 문자열: constants 클래스
- 에러 코드: `ErrorCode` enum

## 5. WebFlux 규칙

- Controller와 Service는 기본적으로 `Mono<T>` 또는 `Flux<T>`를 반환한다.
- `.block()`, `.subscribe()`를 비즈니스 코드에서 직접 호출하지 않는다.
- CPU 비용이 큰 작업은 event-loop에서 실행하지 않는다.
- BCrypt, 파일 처리, blocking SDK 호출이 필요하면 `Schedulers.boundedElastic()`로 격리한다.
- 외부 API 호출은 timeout과 fallback 전략을 함께 둔다.
- 다량의 외부 호출에는 `flatMap(..., concurrency)`로 동시성 제한을 둔다.
- 부분 실패 가능한 API는 전체 브리핑 실패로 번지지 않도록 격리한다.

## 6. 설정 관리 규칙

설정은 기능별 properties 클래스로 분리한다.

예상 설정 그룹:

- `jwt`
- `external.github`
- `external.hackernews`
- `external.devto`
- `external.gemini`
- `briefing`
- `sse`
- `redis`
- `cors`

규칙:

- `@Value`를 여러 클래스에 흩뿌리지 않는다.
- 설정값이 2곳 이상에서 필요하면 반드시 properties 클래스로 만든다.
- 개발용 기본값과 운영용 필수값을 구분한다.
- secret 값은 Git에 커밋하지 않는다.

## 7. DTO와 Entity 규칙

- Entity를 API 응답으로 직접 반환하지 않는다.
- 요청 DTO, 응답 DTO, 내부 command/query 객체를 구분한다.
- 외부 API 응답 DTO는 내부 도메인 모델과 분리한다.
- 외부 API 스펙 변경이 내부 도메인에 직접 전파되지 않도록 adapter에서 변환한다.

## 8. 에러 처리 규칙

- 예외 응답 포맷은 `common.exception` 또는 `common.response`에서 중앙 관리한다.
- Controller마다 try-catch를 반복하지 않는다.
- 도메인별 예외는 의미 있는 이름을 사용한다.
- 외부 API 장애는 로그와 `api_call_log`에 남길 수 있게 설계한다.
- 사용자에게 노출할 메시지와 내부 로그 메시지를 구분한다.
- 예외를 빈 catch 블록으로 삼키지 않는다.
- `Exception`, `RuntimeException` 같은 넓은 예외를 무분별하게 던지지 않는다.
- 에러 응답에 stack trace, SQL, 내부 클래스명, 서버 경로, secret, token, API key를 노출하지 않는다.
- 인증/인가 실패는 401/403을 명확히 구분한다.
- 사용자 입력 오류는 400 계열로 반환하고, 내부 장애는 500 계열로 반환한다.
- 외부 API 장애는 가능한 경우 502/503/504 성격으로 분리하되, 브리핑 생성에서는 partial/fallback 정책을 우선한다.
- 로그에는 원인 추적에 필요한 correlation id, source, status, latency 정도만 남기고 민감정보는 마스킹한다.
- 보안 스캐너가 지적할 수 있는 `printStackTrace`, `System.out.println`, raw exception message 노출은 금지한다.

## 9. Secure Coding 규칙

보안성 검사에서 문제가 될 수 있는 코드를 사전에 막는다.

입력 검증:

- 모든 외부 입력은 DTO에서 Bean Validation으로 검증한다.
- email, password, keyword, owner, repoName, pagination 값은 허용 범위와 길이를 제한한다.
- GitHub owner/repo 같은 값은 허용 문자 패턴을 명시한다.
- enum으로 표현 가능한 값은 문자열 free-form으로 받지 않는다.

인증/인가:

- `/me` 계열 API는 JWT subject 기준으로만 사용자 리소스를 조회한다.
- 클라이언트가 넘긴 `userId`를 신뢰하지 않는다.
- 관리자/사용자 권한이 생기면 method security 또는 명시적 authorization check를 둔다.
- refresh token은 Redis에 저장하고 원문 노출을 피한다.

민감정보:

- password, access token, refresh token, stream token, API key, authorization header는 로그에 남기지 않는다.
- 환경 변수 기본값에 운영 secret을 넣지 않는다.
- 예제 secret은 개발용임을 명확히 표시하고 운영에서는 필수 환경 변수로 받는다.

데이터 접근:

- SQL 문자열 조합으로 사용자 입력을 붙이지 않는다.
- R2DBC repository 또는 bind parameter를 사용한다.
- 정렬 컬럼, 검색 조건처럼 SQL 구조에 영향을 주는 값은 whitelist로 제한한다.

HTTP 보안:

- CORS 허용 origin은 설정으로 관리하고 `*`를 기본값으로 두지 않는다.
- 인증이 필요한 API에는 명확한 SecurityWebFilterChain 규칙을 둔다.
- CSRF 적용 여부는 cookie 기반 인증 도입 시 다시 검토한다.
- 보안 헤더는 기본 Spring Security 설정을 끄지 않는 것을 원칙으로 한다.

암호화/해싱:

- 비밀번호는 BCrypt 등 검증된 password encoder만 사용한다.
- 직접 만든 암호화 알고리즘이나 단순 hash는 금지한다.
- JWT secret은 충분한 길이와 강도를 가진 값을 환경 변수로 주입한다.

파일/경로:

- 사용자 입력으로 파일 경로를 조합하지 않는다.
- 경로 조작 가능성이 있는 기능은 whitelist와 정규화 검증을 먼저 둔다.

운영 안전:

- debug endpoint, actuator 노출, 상세 로그 레벨은 운영 환경에서 제한한다.
- 장애 대응용 로그는 남기되 개인정보와 secret은 남기지 않는다.
- dependency 취약점 점검이 가능하도록 라이브러리 버전을 관리한다.

## 10. Redis 규칙

- Redis key는 중앙 key factory에서 생성한다.
- key 문자열을 각 Service에 직접 작성하지 않는다.
- TTL은 설정값으로 관리한다.
- refresh token, API cache, AI summary cache, progress buffer key prefix를 구분한다.

## 11. 테스트 규칙

- 핵심 규칙은 단위 테스트를 먼저 고려한다.
- WebFlux endpoint는 `WebTestClient`로 테스트한다.
- 외부 API client는 MockWebServer 또는 WireMock으로 timeout, 500, 429를 검증한다.
- Repository는 R2DBC 기준 저장/조회/unique 제약을 검증한다.
- 새로운 유스케이스를 추가할 때 성공 케이스와 실패 케이스를 함께 둔다.

## 12. Karpathy Rule

이 프로젝트에서 말하는 Karpathy Rule은 "AI가 코드를 많이 만들수록 코드는 더 작고, 명확하고, 직접 읽을 수 있어야 한다"는 기준이다.

- clever code보다 boring code를 우선한다.
- 한 번에 큰 추상화를 만들지 않는다.
- 작은 함수, 작은 클래스, 작은 PR 단위로 나눈다.
- 이름만 봐도 역할이 드러나게 작성한다.
- 불필요한 generic, reflection, dynamic magic, 과한 factory 패턴을 피한다.
- 같은 코드가 2번 나오면 지켜보고, 3번 나오면 추상화를 검토한다.
- 추상화는 먼저 실제 중복과 변경 이유가 확인된 뒤 만든다.
- 디버깅 가능한 코드를 우선한다.
- 로그, 에러 코드, 테스트를 통해 실패 지점을 추적 가능하게 만든다.
- AI가 만든 코드는 사람이 설명할 수 있어야 merge 가능한 코드로 본다.

## 13. AI 코드 작성 체크리스트

AI가 백엔드 코드를 작성하기 전 반드시 확인한다.

- 이 코드는 어느 패키지에 속해야 하는가?
- 설정값을 하드코딩하지 않았는가?
- Controller가 너무 뚱뚱해지지 않았는가?
- Entity를 그대로 API 응답에 노출하지 않았는가?
- `.block()` 또는 직접 `.subscribe()`를 쓰지 않았는가?
- 외부 API timeout/fallback이 있는가?
- Redis key와 TTL이 중앙에서 관리되는가?
- 테스트하기 쉬운 구조인가?
- 에러 응답에 내부 정보나 민감정보가 노출되지 않는가?
- 예외를 삼키거나 raw exception message를 그대로 반환하지 않았는가?
- 사용자 입력 검증과 인가 검증이 있는가?
- 보안 스캐너에 걸릴 만한 로그, secret, SQL 조합, debug 코드가 없는가?
- Karpathy Rule 기준으로 너무 크거나 똑똑한 척하는 코드가 아닌가?
