# API Contract

이 문서는 `오늘의 개발` 백엔드와 프론트엔드가 반드시 공유해야 하는 API 계약입니다. 구현 중 백엔드 응답 구조, 에러 포맷, 상태값, SSE 이벤트 이름을 임의로 바꾸지 않습니다.

## 1. 기본 원칙

- API 계약은 백엔드와 프론트엔드 모두 동일하게 따른다.
- 계약 변경이 필요하면 백엔드/프론트 문서를 함께 수정한 뒤 구현한다.
- 응답 필드 이름은 camelCase를 사용한다.
- 날짜/시간은 ISO-8601 문자열을 사용한다.
- 금액, 점수, 개수처럼 의미가 있는 숫자는 단위를 문서나 필드명으로 명확히 한다.
- `null`과 빈 배열/빈 문자열의 의미를 구분한다.
- Entity를 그대로 응답하지 않고 API 응답 DTO를 사용한다.

## 2. 공통 성공 응답

단건 응답:

```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

목록 응답:

```json
{
  "success": true,
  "data": [],
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

페이징 응답:

```json
{
  "success": true,
  "data": {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

규칙:

- 성공 응답은 `success: true`를 사용한다.
- 실제 응답 본문은 항상 `data` 안에 둔다.
- 삭제 성공처럼 본문이 거의 없는 경우에도 `data`는 `null` 또는 간단한 결과 객체로 명시한다.

## 3. 공통 에러 응답

```json
{
  "success": false,
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
    "details": [],
    "traceId": "01HX0000000000000000000000"
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

Validation 에러:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "요청 값이 올바르지 않습니다.",
    "details": [
      {
        "field": "email",
        "reason": "올바른 이메일 형식이 아닙니다."
      }
    ],
    "traceId": "01HX0000000000000000000000"
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

보안 규칙:

- stack trace를 응답에 포함하지 않는다.
- exception class name을 응답에 포함하지 않는다.
- SQL, 내부 파일 경로, 서버 경로를 응답에 포함하지 않는다.
- token, password, API key, authorization header를 응답에 포함하지 않는다.
- raw exception message를 그대로 사용자에게 전달하지 않는다.

## 4. HTTP Status 기준

- `200 OK`: 조회/수정 성공
- `201 Created`: 생성 성공
- `202 Accepted`: 긴 작업 생성 요청 접수
- `204 No Content`: 본문 없는 삭제 성공이 필요할 때만 사용
- `400 Bad Request`: 요청 형식 또는 값 오류
- `401 Unauthorized`: 인증 없음 또는 access token 만료/invalid
- `403 Forbidden`: 인증은 됐지만 권한 없음
- `404 Not Found`: 리소스 없음
- `409 Conflict`: 중복 요청, 이미 진행 중인 브리핑, 중복 관심사
- `429 Too Many Requests`: rate limit
- `500 Internal Server Error`: 서버 내부 장애
- `502 Bad Gateway`: 외부 API 오류
- `503 Service Unavailable`: 외부 서비스 일시 불가
- `504 Gateway Timeout`: 외부 API timeout

## 5. Error Code 기준

공통:

- `VALIDATION_FAILED`
- `INVALID_REQUEST`
- `RESOURCE_NOT_FOUND`
- `CONFLICT`
- `INTERNAL_SERVER_ERROR`

인증:

- `AUTH_INVALID_CREDENTIALS`
- `AUTH_TOKEN_MISSING`
- `AUTH_TOKEN_INVALID`
- `AUTH_TOKEN_EXPIRED`
- `AUTH_REFRESH_TOKEN_INVALID`
- `AUTH_FORBIDDEN`

관심사:

- `PREFERENCE_KEYWORD_DUPLICATED`
- `PREFERENCE_KEYWORD_NOT_FOUND`
- `PREFERENCE_REPOSITORY_DUPLICATED`
- `PREFERENCE_REPOSITORY_NOT_FOUND`
- `PREFERENCE_REPOSITORY_FORMAT_INVALID`

브리핑:

- `BRIEFING_NOT_FOUND`
- `BRIEFING_ALREADY_IN_PROGRESS`
- `BRIEFING_CREATE_FAILED`
- `BRIEFING_PARTIAL_CREATED`
- `BRIEFING_SUMMARY_FAILED`

저장한 글:

- `SAVED_ARTICLE_DUPLICATED`
- `SAVED_ARTICLE_NOT_FOUND`

외부 API:

- `EXTERNAL_GITHUB_FAILED`
- `EXTERNAL_HACKER_NEWS_FAILED`
- `EXTERNAL_DEVTO_FAILED`
- `EXTERNAL_RATE_LIMITED`
- `EXTERNAL_TIMEOUT`

AI:

- `AI_SUMMARY_FAILED`
- `AI_RATE_LIMITED`
- `AI_TIMEOUT`

SSE:

- `STREAM_TOKEN_INVALID`
- `STREAM_TOKEN_EXPIRED`
- `STREAM_NOT_FOUND`

## 6. 공통 Enum

Briefing status:

```text
GENERATING
COMPLETED
PARTIAL
SUMMARY_FAILED
FAILED
```

Source:

```text
GITHUB
HACKER_NEWS
DEVTO
AI
```

Progress step:

```text
BRIEFING_REQUESTED
GITHUB_COLLECTING
GITHUB_COLLECTED
HACKER_NEWS_COLLECTING
HACKER_NEWS_COLLECTED
DEVTO_COLLECTING
DEVTO_COLLECTED
FILTERING
SCORING
AI_SUMMARIZING
SAVING
DONE
PARTIAL_DONE
FAILED
```

## 7. Auth API

### POST `/api/auth/signup`

Request:

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com"
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### POST `/api/auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "password123!"
}
```

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "access-token",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "userId": 1,
      "email": "user@example.com"
    }
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

규칙:

- refresh token은 가능하면 HttpOnly cookie로 내려준다.
- access token은 응답 body에 포함할 수 있다.
- password hash는 절대 응답하지 않는다.

### POST `/api/auth/refresh`

Response `200`:

```json
{
  "success": true,
  "data": {
    "accessToken": "new-access-token",
    "tokenType": "Bearer",
    "expiresIn": 1800
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### POST `/api/auth/logout`

Response `200`:

```json
{
  "success": true,
  "data": {
    "loggedOut": true
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

## 8. Preferences API

### 수집 정책

- 관심 키워드는 대소문자를 구분하지 않는다.
- 서버는 키워드를 내부적으로 lowercase로 정규화해 저장하고 중복을 판정한다.
- `Spring`, `spring`, `SPRING`은 같은 관심사로 본다.
- 브리핑 생성 시 DEV.to는 관심 키워드 tag를 기준으로 수집한다.
- 브리핑 생성 시 GitHub는 사용자가 등록한 repository가 없어도 관심 키워드로 repository를 자동 검색한 뒤 release를 수집한다.
- GitHub repository 직접 등록은 필수가 아니라 특정 저장소를 우선 보고 싶을 때 쓰는 선택 설정이다.
- Hacker News는 top stories를 수집한 뒤 관심 키워드 매칭과 점수화로 우선순위를 조정한다.

### GET `/api/preferences/me`

Response:

```json
{
  "success": true,
  "data": {
    "keywords": [
      {
        "keywordId": 1,
        "keyword": "Spring",
        "weight": 5,
        "createdAt": "2026-05-21T09:00:00+09:00"
      }
    ],
    "repositories": [
      {
        "repositoryId": 1,
        "owner": "spring-projects",
        "repoName": "spring-framework",
        "createdAt": "2026-05-21T09:00:00+09:00"
      }
    ]
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### POST `/api/preferences/me/keywords`

Request:

```json
{
  "keyword": "WebFlux",
  "weight": 5
}
```

### DELETE `/api/preferences/me/keywords/{keywordId}`

Response:

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### POST `/api/preferences/me/repositories`

Request:

```json
{
  "owner": "spring-projects",
  "repoName": "spring-framework"
}
```

규칙:

- `owner/repoName` 문자열 하나로 받지 않고 `owner`, `repoName`을 분리한다.
- 프론트에서 `owner/repo` 입력을 제공하더라도 API 요청 전 분리한다.

## 9. Briefings API

### POST `/api/briefings`

Response `202`:

```json
{
  "success": true,
  "data": {
    "briefingId": 100,
    "status": "GENERATING",
    "createdAt": "2026-05-21T09:00:00+09:00"
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

규칙:

- 브리핑 생성은 오래 걸릴 수 있으므로 요청 접수 후 `202 Accepted`를 우선 사용한다.
- 이미 진행 중인 브리핑이 있으면 `409`와 `BRIEFING_ALREADY_IN_PROGRESS`를 반환한다.
- 9단계부터 요청 접수 직후 `GENERATING` 상태와 `briefingId`를 반환하고, 실제 수집/요약/저장은 background worker가 수행한다.
- 생성 진행률과 완료 상태는 아래 SSE API에서 조회한다.

### GET `/api/briefings`

Query:

```text
page=0&size=20
```

Response:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "briefingId": 100,
        "title": "오늘의 개발 브리핑",
        "summary": "오늘은 Spring과 AI 도구 업데이트가 주요 흐름입니다.",
        "status": "COMPLETED",
        "generatedAt": "2026-05-21T09:00:00+09:00",
        "itemCount": 12
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### GET `/api/briefings/{briefingId}`

Response:

```json
{
  "success": true,
  "data": {
    "briefingId": 100,
    "title": "오늘의 개발 브리핑",
    "summary": "오늘은 Spring과 AI 도구 업데이트가 주요 흐름입니다.",
    "status": "COMPLETED",
    "generatedAt": "2026-05-21T09:00:00+09:00",
    "sections": [
      {
        "source": "GITHUB",
        "status": "COMPLETED",
        "items": [
          {
            "itemId": 1,
            "source": "GITHUB",
            "externalId": "release-1",
            "title": "Spring Framework Release",
            "url": "https://github.com/spring-projects/spring-framework",
            "summary": "주요 변경사항 요약",
            "score": 92.5,
            "publishedAt": "2026-05-21T09:00:00+09:00",
            "metadata": {
              "stars": 1234,
              "comments": 12,
              "tags": ["spring", "java"]
            },
            "saved": false
          }
        ]
      }
    ]
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

규칙:

- source별 추가 정보는 `metadata`에 둔다.
- 프론트는 `source`에 따라 metadata를 다르게 표현할 수 있다.
- `metadata`에는 민감정보를 넣지 않는다.
- `publishedAt`은 외부 source의 원본 발행 시간을 저장한 값이다.
- `metadata`는 `briefing_item.metadata` JSONB에 저장된 source별 공개 부가 정보다.
- 원본 발행 시간이 없는 예외 케이스는 서버가 브리핑 생성 시간으로 fallback할 수 있다.

## 10. SSE API

### POST `/api/briefings/{briefingId}/stream-token`

Response:

```json
{
  "success": true,
  "data": {
    "streamToken": "short-lived-stream-token",
    "expiresIn": 180
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

규칙:

- stream token은 짧은 만료 시간을 가진다.
- stream token은 Redis 검증 시 즉시 소비되는 일회성 토큰이다.
- 일반 access token을 SSE query string에 넣지 않는다.

### GET `/api/briefings/{briefingId}/stream?streamToken=...`

Event: `BRIEFING_PROGRESS`

```json
{
  "briefingId": 100,
  "step": "GITHUB_COLLECTED",
  "source": "GITHUB",
  "processed": 10,
  "total": 10,
  "message": "GitHub 릴리즈 수집 완료"
}
```

Event: `BRIEFING_DONE`

```json
{
  "briefingId": 100,
  "status": "COMPLETED",
  "message": "브리핑 생성이 완료되었습니다."
}
```

Event: `BRIEFING_PARTIAL_DONE`

```json
{
  "briefingId": 100,
  "status": "PARTIAL",
  "message": "일부 출처를 제외하고 브리핑을 생성했습니다.",
  "failedSources": ["DEVTO"]
}
```

Event: `BRIEFING_FAILED`

```json
{
  "briefingId": 100,
  "status": "FAILED",
  "message": "브리핑 생성에 실패했습니다."
}
```

SSE 규칙:

- 이벤트 이름은 상수로 관리한다.
- `BRIEFING_REQUESTED`, `FILTERING`, `SCORING`, `AI_SUMMARIZING`, `SAVING`, `DONE`처럼 특정 출처 또는 개수 정보가 없는 단계는 `source`, `processed`, `total`이 `null`일 수 있다.
- `BRIEFING_DONE`, `BRIEFING_PARTIAL_DONE`, `BRIEFING_FAILED` 수신 후 프론트는 연결을 닫는다.
- SSE message에는 token, 내부 exception, stack trace를 포함하지 않는다.

## 11. Schedule API

### GET `/api/schedule/me/briefing`

인증된 사용자의 자동 브리핑 수신 설정을 조회한다.

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "briefingTime": "08:00",
    "timezone": "Asia/Seoul",
    "enabled": true,
    "updatedAt": "2026-06-07T13:20:00"
  },
  "timestamp": "2026-06-07T13:20:00+09:00"
}
```

정책:
- 설정이 없는 사용자는 기본값 `08:00`, `Asia/Seoul`, `enabled=true`를 생성해 반환한다.
- 프론트는 `briefingTime`을 사이드바의 "매일 받을 시간"에 표시한다.
- 타임존은 IANA timezone ID를 사용한다. 예: `Asia/Seoul`, `America/New_York`.

### PUT `/api/schedule/me/briefing`

사용자의 자동 브리핑 수신 설정을 수정한다.

Request:

```json
{
  "briefingTime": "08:30",
  "timezone": "Asia/Seoul",
  "enabled": true
}
```

Response `200 OK`:

```json
{
  "success": true,
  "data": {
    "briefingTime": "08:30",
    "timezone": "Asia/Seoul",
    "enabled": true,
    "updatedAt": "2026-06-07T13:25:00"
  },
  "timestamp": "2026-06-07T13:25:00+09:00"
}
```

Validation:
- `briefingTime`: `HH:mm` 형식의 시간이다.
- `timezone`: 유효한 IANA timezone ID여야 한다.
- `enabled`: 자동 브리핑 활성 여부다.

자동 생성 정책:
- 백엔드 Scheduler는 주기적으로 실행 대상 사용자를 확인한다.
- 사용자의 로컬 시간이 `briefingTime`과 같은 분이면 브리핑 생성 job을 enqueue한다.
- 같은 사용자에게 같은 로컬 날짜의 브리핑이 이미 있으면 중복 생성하지 않는다.
- 실제 수집과 AI 요약은 기존 브리핑 생성 job worker가 처리한다.
## 12. Saved Articles API

### POST `/api/saved-articles/{itemId}`

Request:

```json
{
  "memo": "나중에 자세히 읽기"
}
```

Response `201`:

```json
{
  "success": true,
  "data": {
    "savedId": 1,
    "itemId": 1,
    "title": "Spring Framework Release",
    "url": "https://github.com/spring-projects/spring-framework",
    "source": "GITHUB",
    "memo": "나중에 자세히 읽기",
    "savedAt": "2026-05-21T09:00:00+09:00"
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### GET `/api/saved-articles`

Response:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "savedId": 1,
        "itemId": 1,
        "title": "Spring Framework Release",
        "url": "https://github.com/spring-projects/spring-framework",
        "source": "GITHUB",
        "memo": "나중에 자세히 읽기",
        "savedAt": "2026-05-21T09:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### PATCH `/api/saved-articles/{savedId}`

Request:

```json
{
  "memo": "다시 읽고 팀에 공유하기"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "savedId": 1,
    "itemId": 1,
    "title": "Spring Framework Release",
    "url": "https://github.com/spring-projects/spring-framework",
    "source": "GITHUB",
    "memo": "다시 읽고 팀에 공유하기",
    "savedAt": "2026-05-21T09:00:00+09:00"
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

### DELETE `/api/saved-articles/{savedId}`

Response:

```json
{
  "success": true,
  "data": {
    "deleted": true
  },
  "timestamp": "2026-05-21T09:00:00+09:00"
}
```

규칙:

- 본인의 브리핑 아이템만 저장할 수 있다.
- 본인이 저장한 글만 조회, 수정, 삭제할 수 있다.
- 같은 `itemId`를 중복 저장하면 `409`와 `SAVED_ARTICLE_DUPLICATED`를 반환한다.
- 존재하지 않거나 권한이 없는 저장 글은 `SAVED_ARTICLE_NOT_FOUND`로 처리한다.
- `memo`는 최대 1000자이며, 비어 있으면 빈 문자열로 저장할 수 있다.

## 12. 프론트 타입 생성 기준

프론트는 이 계약을 기준으로 타입을 만든다.

권장 타입:

```ts
type ApiSuccess<T> = {
  success: true;
  data: T;
  timestamp: string;
};

type ApiError = {
  success: false;
  error: {
    code: string;
    message: string;
    details: Array<{ field?: string; reason: string }>;
    traceId: string;
  };
  timestamp: string;
};

type ApiResponse<T> = ApiSuccess<T> | ApiError;
```

규칙:

- 프론트는 `success` 값으로 성공/실패를 좁힌다.
- error code는 문자열 비교를 흩뿌리지 않고 상수로 관리한다.
- 백엔드 DTO 변경 시 프론트 타입도 함께 수정한다.

## 13. 변경 관리

- 이 문서 변경은 API breaking change로 간주한다.
- endpoint, field name, enum, error code 변경 시 백엔드/프론트 문서를 모두 수정한다.
- 구현 중 임시 응답을 만들 경우 `TODO contract` 주석을 남기고 작업 완료 전에 제거한다.
- 계약과 구현이 다르면 계약을 먼저 수정하고 구현을 맞춘다.

---
## 운영 인증/CORS 설정 기준

### Refresh Cookie

- refresh cookie 속성은 `app.auth.refresh-cookie` 설정으로 중앙 관리한다.
- 로컬 기본값은 `secure=false`, `httpOnly=true`, `sameSite=Lax`, `path=/api/auth`다.
- 운영 HTTPS 환경에서는 `app.auth.refresh-cookie.secure=true`로 설정한다.
- `sameSite=None`을 사용하는 경우 `secure=true`가 아니면 서버 기동 단계에서 차단한다.

### CORS

- CORS origin은 `app.cors.allowed-origins`에서 중앙 관리한다.
- `allow-credentials=true`를 사용하는 경우 `allowed-origins=*`는 허용하지 않는다.
- 운영에서는 실제 프론트 URL만 origin으로 등록한다.
- Authorization header, refresh token, stream token, API key는 URL/query/log/response DTO에 넣지 않는다.

