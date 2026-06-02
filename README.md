# Todaydev Backend

개발자가 매일 확인해야 하는 GitHub, Hacker News, DEV.to 정보를 관심사 기준으로 수집하고 AI로 요약하는 WebFlux 기반 백엔드입니다.

## 현재 구현 범위

- 공통 API 응답/에러/traceId
- WebFlux Security, JWT access token, HttpOnly refresh token
- 관심 키워드와 GitHub repository 관리
- 외부 API adapter: GitHub, Hacker News, DEV.to
- 브리핑 후보 생성, 점수화, 중복 제거
- 비동기 브리핑 생성 pipeline
- Gemini AI 요약과 fallback summary
- SSE 진행률 stream과 1회용 stream token
- 저장한 글 API

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

## 로컬 실행 준비

필수:

- JDK 17
- Docker Desktop
- Docker Compose
- PowerShell

선택:

- Gemini API key
- GitHub PAT

## 로컬 서비스 실행

PostgreSQL과 Redis를 실행합니다.

```powershell
docker compose up -d
docker compose ps
```

현재 로컬 포트:

- Backend: `http://localhost:8080`
- PostgreSQL: `localhost:15432`
- Redis: `localhost:6379`

백엔드를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

테스트를 실행합니다.

```powershell
.\gradlew.bat test --console=plain
```

## 환경 변수와 로컬 설정

Tracked source에는 secret을 저장하지 않습니다.

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

- `application-local.yml`은 `.gitignore`에 포함되어야 합니다.
- API key, token, Authorization header를 query string, 로그, 응답 DTO에 넣지 않습니다.
- 운영 환경에서는 refresh cookie `secure(true)` 전환이 필요합니다.

## 주요 설정 위치

- API 계약: `API_CONTRACT.md`
- 개발 계획: `DEVELOPMENT_PLAN.md`
- 코딩 규칙: `CODING_RULES.md`
- 테스트 규칙: `TESTING_RULES.md`
- 완료 기준: `DONE_DEFINITION.md`
- 성능/검증 기준: `PERFORMANCE_VALIDATION.md`
- 애플리케이션 설정: `src/main/resources/application.yml`
- 로컬 secret 설정: `application-local.yml`

## API 빠른 확인

PowerShell에서는 JSON body를 직접 quote로 조립하지 말고 `ConvertTo-Json -Compress`를 사용합니다.

회원가입:

```powershell
$body=@{email="test@example.com";password="password123!"}|ConvertTo-Json -Compress; Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/signup" -ContentType "application/json" -Body $body
```

로그인:

```powershell
$body=@{email="test@example.com";password="password123!"}|ConvertTo-Json -Compress; $login=Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/auth/login" -ContentType "application/json" -Body $body; $headers=@{Authorization="Bearer $($login.data.accessToken)"}
```

관심 키워드 등록:

```powershell
$body=@{keyword="webflux";weight=8}|ConvertTo-Json -Compress; Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/preferences/me/keywords" -Headers $headers -ContentType "application/json" -Body $body
```

관심 repository 등록:

```powershell
$body=@{owner="spring-projects";repoName="spring-framework"}|ConvertTo-Json -Compress; Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/preferences/me/repositories" -Headers $headers -ContentType "application/json" -Body $body
```

브리핑 생성 요청:

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
- CORS origin을 운영 프론트 URL로 제한
- refresh cookie `secure(true)` 적용
- DB/Redis 계정과 비밀번호 교체
- DEBUG logging 제거 또는 축소
- 성능 기준은 `PERFORMANCE_VALIDATION.md`에 따라 재측정

---
## 운영 인증/CORS 체크

로컬 기본 설정은 개발 편의를 기준으로 둡니다. 운영 배포 전에는 아래 값을 반드시 운영 환경 설정이나 secret manager에서 덮어씁니다.

```yaml
app:
  auth:
    refresh-cookie:
      secure: true
      http-only: true
      same-site: Lax
      path: /api/auth
  cors:
    allowed-origins:
      - https://your-frontend.example.com
    allow-credentials: true
```

운영 기준:

- `JWT_SECRET`은 기본값을 사용하지 않고 충분히 긴 운영 secret으로 교체합니다.
- refresh cookie는 HTTPS 운영 환경에서 `secure=true`로 설정합니다.
- `same-site=None`을 쓰는 경우 반드시 `secure=true`를 함께 사용합니다.
- `allow-credentials=true`일 때 `allowed-origins=*`는 서버 기동 단계에서 차단됩니다.
- CORS origin은 운영 프론트 URL만 허용합니다.
- API key, token, Authorization header, refresh token은 URL/query/log/response DTO에 넣지 않습니다.
