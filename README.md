# Todaydev Backend

Todaydev는 개발자가 매일 확인해야 하는 GitHub Release, Hacker News, DEV.to 기술 소식을 관심 키워드와 GitHub 저장소 기준으로 수집하고, 점수화와 AI 요약을 거쳐 브리핑으로 제공하는 개발자 맞춤 정보 큐레이션 서비스입니다.

백엔드는 오래 걸리는 브리핑 생성 작업을 일반 HTTP 요청과 분리하고, 외부 API/AI API 실패가 전체 서비스 실패로 번지지 않도록 설계하는 데 초점을 두었습니다.

## 핵심 기능

- JWT access token과 HttpOnly refresh token 기반 인증
- 관심 키워드와 GitHub watched repository 관리
- GitHub, Hacker News, DEV.to 외부 API 수집
- WebFlux 기반 source별 병렬 수집
- rule-based scoring과 중복 제거
- Gemini AI 요약 및 fallback summary
- `202 Accepted` 기반 비동기 브리핑 생성
- Redis 기반 중복 생성 lock, stream token, progress buffer, API cache
- SSE 기반 브리핑 생성 진행률 전송
- 저장한 글 조회와 메모 관리

## 기술 스택

- Java 17
- Spring Boot 4.0.6
- Spring WebFlux
- Spring Security
- Spring Data R2DBC
- Spring Data Reactive Redis
- PostgreSQL 16
- Redis 7
- Flyway
- Gradle

## 설계 포인트

### 긴 작업과 요청 응답 분리

브리핑 생성은 외부 API 수집, 점수화, AI 요약, DB 저장을 포함하는 긴 작업입니다. 이를 HTTP 요청 안에서 모두 처리하면 timeout 위험이 있고 사용자는 진행 상태를 알 수 없습니다.

그래서 생성 요청 시에는 `GENERATING` 상태의 브리핑을 먼저 만들고 `202 Accepted`로 응답합니다. 실제 생성은 background job에서 처리하며, 진행 상황은 SSE로 전달합니다.

### WebFlux 기반 병렬 수집

GitHub, Hacker News, DEV.to, Gemini 호출은 CPU 연산보다 I/O 대기가 큰 작업입니다. WebClient와 Reactor를 사용해 source별 수집을 병렬화하고, 특정 source 실패가 전체 브리핑 실패로 번지지 않도록 결과를 분리했습니다.

### Redis 역할 분리

Redis는 단순 캐시뿐 아니라 TTL 기반 임시 상태 저장소로 사용합니다.

- Refresh Token: 로그아웃 및 서버 측 무효화
- Stream Token: SSE 연결용 단기 1회용 토큰
- Progress Buffer: 늦게 연결한 클라이언트에게 이전 진행 이벤트 재생
- In-progress Lock: 같은 사용자의 중복 브리핑 생성 방지
- API Cache: 외부 API 반복 호출 비용과 rate limit 노출 감소

### AI 실패 격리

AI 요약은 서비스 가치를 높이는 기능이지만, AI provider 장애가 전체 브리핑 실패로 이어지면 안 됩니다. Gemini 호출 실패 시 fallback summary를 저장하고 브리핑 상태를 `SUMMARY_FAILED`로 분리해, 사용자가 수집된 글은 계속 확인할 수 있도록 했습니다.

## 성능 측정과 개선

성능 개선은 추측으로 진행하지 않고, 측정 후 병목을 분리하는 방식으로 진행했습니다. 로컬 단일 인스턴스 환경에서 측정했기 때문에 절대 처리량보다 전후 변화, p95/p99, 병목 이동을 중심으로 해석했습니다.

### 1. 외부 API 응답 Redis 캐시

최종 브리핑은 사용자 관심사와 생성 시점에 따라 달라져 cache hit율이 낮다고 판단했습니다. 대신 여러 사용자가 공유할 수 있는 외부 API 응답을 `source + query` 단위로 캐싱했습니다.

| 항목 | 캐시 전 | 캐시 적용 후 | 결과 |
| --- | ---: | ---: | --- |
| 최종 완료 평균 | 3.08s | 1.37s | 약 56% 단축 |
| 최종 완료 p95 | 3.99s | 2.38s | 약 41% 단축 |
| Hacker News 평균 | 833.9ms | 8.6ms | 반복 수집 비용 감소 |
| DEV.to 평균 | 70.3ms | 9.1ms | 반복 태그 검색 비용 감소 |
| GitHub 평균 | 1731.8ms | 494.6ms | 검색/릴리즈 반복 호출 비용 감소 |

캐시 적용 후 빈 리스트가 `Flux.empty()`로 흘러 `switchIfEmpty`가 다시 동작하는 negative caching 버그도 발견했습니다. 정상 성공 응답의 빈 결과만 캐싱하고, timeout/error는 캐싱하지 않도록 수정해 일시 장애 결과가 TTL 동안 고정되는 문제를 피했습니다.

### 2. 순차 수집 vs 병렬 수집

외부 API 수집 파이프라인의 병렬화 효과를 확인하기 위해 실제 외부 네트워크 대신 고정 지연 mock을 사용했습니다. 목적은 네트워크 속도 측정이 아니라 병렬화 구조 자체의 효과를 분리하는 것이었습니다.

| 구분 | avg | p95 | 해석 |
| --- | ---: | ---: | --- |
| 순차 비교 경로 | 1835ms | 1847ms | source 지연의 합에 가까움 |
| 실제 collector 병렬 경로 | 814ms | 830ms | 가장 느린 source 지연에 수렴 |

평균 기준 약 55.6% 단축되었습니다. 이를 통해 source 수집 병렬화가 전체 브리핑 생성 시간을 "각 지연의 합"에서 "가장 느린 source 지연"에 가깝게 줄인다는 점을 확인했습니다.

### 3. 목록 조회 page-first 쿼리 개선

기존 목록 조회는 화면에 20개만 보여주면 되는데도, 사용자 브리핑 전체와 `briefing_item`을 먼저 join/grouping한 뒤 `LIMIT`을 적용할 수 있는 구조였습니다. 데이터가 늘면 목록 조회가 전체 item 수에 끌려갈 위험이 있었습니다.

이를 먼저 사용자 브리핑 page 20개를 CTE로 선별한 뒤, 해당 20개에 대해서만 item count를 계산하는 page-first 방식으로 변경했습니다.

| 항목 | 기존 | 개선 |
| --- | --- | --- |
| 처리 순서 | 전체 briefing + item join 후 LIMIT | page 20개 선별 후 item count |
| `briefing_item` 접근 | 27,435행 seq scan | 500행 index scan |
| Buffer hit | 1,233 | 89 |
| EXPLAIN 실행 시간 | 190.6ms | 0.50ms |

E2E k6 결과에서는 SQL 개선 폭이 그대로 p99 개선으로 비례하지 않았습니다. `http_req_waiting`뿐 아니라 `http_req_blocked/connecting`도 함께 커졌기 때문에, 고부하에서는 SQL 밖의 런타임 자원과 로컬 연결 계층 경합이 다음 병목으로 드러났다고 해석했습니다.

### 4. R2DBC connection pool 튜닝

R2DBC pool 기본값은 `initialSize=10`, `maxSize=10`이었습니다. 상세 조회 750 VU에서 무부하 p99는 27ms 수준이었지만, 부하 상황에서는 p99가 1.68s까지 증가했습니다. 이를 connection pool 대기 가설로 보고 pool 크기만 단계적으로 조정해 재측정했습니다.

| 조건 | RPS | p95 | p99 | waiting p99 | blocked/connecting p99 |
| --- | ---: | ---: | ---: | ---: | ---: |
| pool 10 | 584.6 req/s | 636.8ms | 1682.0ms | 1203.6ms | 214.4ms |
| pool 30 | 645.5 req/s | 265.4ms | 698.7ms | 582.0ms | 328.6ms |
| pool 50 | 651.6 req/s | 229.9ms | 439.1ms | 365.3ms | 350.7ms |

pool을 키우면서 `waiting p99`는 줄었지만 `blocked/connecting p99`는 증가했습니다. 이는 병목이 R2DBC pool 대기에서 로컬 HTTP 연결 계층/측정 환경 쪽으로 이동했다는 의미입니다.

현재 로컬 단일 인스턴스 조건에서는 `maxSize=50`까지 상세 조회 p99 개선이 확인되었습니다. 다만 운영 환경에서는 고정값으로 받아들이면 안 되며, `앱 인스턴스 수 * pool size <= PostgreSQL max_connections - 운영 여유분` 기준으로 재산정해야 합니다.

## 로컬 실행

필수 준비:

- JDK 17
- Docker Desktop
- Docker Compose
- PowerShell

PostgreSQL과 Redis를 실행합니다.

```powershell
docker compose up -d
docker compose ps
```

백엔드를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

테스트를 실행합니다.

```powershell
.\gradlew.bat test --console=plain
```

기본 로컬 포트:

- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:15432`
- Redis: `localhost:6379`

## 로컬 환경 변수

tracked source에는 secret을 저장하지 않습니다.

권장 방식:

```powershell
[Environment]::SetEnvironmentVariable('GEMINI_API_KEY', '<your-key>', 'User')
[Environment]::SetEnvironmentVariable('GITHUB_PAT', '<your-token>', 'User')
```

또는 Git에서 제외된 `application-local.yml`을 backend 루트에 둡니다.

```yaml
external:
  gemini:
    api-key: your-local-key
  github:
    token: your-local-token
```

주의:

- API key, token, Authorization header를 query string, 로그, 응답 DTO에 남기지 않습니다.
- 운영 환경에서는 refresh cookie `secure=true` 전환이 필요합니다.
- CORS origin은 운영 프론트엔드 URL로 제한해야 합니다.

## 주요 문서

- API 계약: `API_CONTRACT.md`
- 개발 계획: `DEVELOPMENT_PLAN.md`
- 코딩 규칙: `CODING_RULES.md`
- 테스트 규칙: `TESTING_RULES.md`
- 성능 검증 계획: `PERFORMANCE_VALIDATION.md`
- 애플리케이션 설정: `src/main/resources/application.yml`
- 로컬 secret 설정: `application-local.yml`

## API 빠른 확인

PowerShell에서는 JSON body를 직접 quote로 조립하지 않고 `ConvertTo-Json -Compress`를 사용합니다.

회원가입:

```powershell
$body=@{email="test@example.com";password="password123!"}|ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/signup" -ContentType "application/json" -Body $body
```

로그인:

```powershell
$body=@{email="test@example.com";password="password123!"}|ConvertTo-Json -Compress
$login=Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" -ContentType "application/json" -Body $body
$headers=@{Authorization="Bearer $($login.data.accessToken)"}
```

관심 키워드 등록:

```powershell
$body=@{keyword="webflux";weight=8}|ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/preferences/me/keywords" -Headers $headers -ContentType "application/json" -Body $body
```

브리핑 생성:

```powershell
$briefing=Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/briefings" -Headers $headers
```

SSE stream token 발급:

```powershell
$token=Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/briefings/$($briefing.data.briefingId)/stream-token" -Headers $headers
```

SSE 수신:

```powershell
curl.exe -N "http://localhost:8080/api/briefings/$($briefing.data.briefingId)/stream?streamToken=$($token.data.streamToken)"
```

저장한 글 조회:

```powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8080/api/saved-articles?page=0&size=20" -Headers $headers
```

## 운영 전 체크

- `JWT_SECRET`을 운영 secret으로 교체
- `GEMINI_API_KEY`, `GITHUB_PAT`를 secret manager 또는 안전한 환경 변수로 주입
- CORS origin을 운영 프론트엔드 URL로 제한
- refresh cookie `secure=true` 적용
- DB/Redis 계정과 비밀번호 교체
- DEBUG logging 제거 또는 축소
- R2DBC pool size를 DB `max_connections`, 앱 인스턴스 수, DB CPU 기준으로 재산정
- 부하 생성기와 서버를 분리한 환경에서 p95/p99 재측정
