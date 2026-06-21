# 운영 배포 전/후 변경 필요 항목

현재 `todaydev-backend`와 `todaydev-frontend` 상태를 기준으로, notification 기능이 운영에서 정상 동작하려면 아래 항목을 운영 환경에 맞게 변경해야 한다.

## 1. 프론트 API 주소

프론트 배포 환경에서 백엔드 API 주소를 운영 백엔드 도메인으로 변경한다.

```env
VITE_API_BASE_URL=https://운영-백엔드-도메인
```

적용 위치:

- `todaydev-frontend` 배포 환경변수
- 또는 `.env.production`

현재 `.env.example`은 로컬 백엔드를 가리킨다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

## 2. 백엔드 CORS 설정

현재 백엔드는 로컬 프론트 주소만 허용한다.

운영에서는 프론트 배포 도메인을 허용해야 한다.

```yaml
app:
  cors:
    allowed-origins:
      - https://운영-프론트-도메인
```

주의:

- `allow-credentials: true` 상태에서는 `allowed-origins: "*"`를 사용할 수 없다.
- 프론트와 백엔드 도메인이 다르면 CORS와 cookie 설정을 함께 맞춰야 한다.

## 3. Refresh Cookie 운영값

운영 HTTPS 환경에서는 refresh cookie를 보안 설정으로 변경한다.

```yaml
app:
  auth:
    refresh-cookie:
      secure: true
      http-only: true
      same-site: None
      path: /api/auth
```

참고:

- 프론트와 백엔드가 같은 사이트 정책 안에 있으면 `same-site: Lax`도 가능하다.
- `same-site: None`을 사용하려면 반드시 `secure: true`여야 한다.

## 4. JWT Secret

개발 기본값을 사용하면 안 된다.

운영에서는 32자 이상의 랜덤 문자열을 주입한다.

```env
JWT_SECRET=운영용_32자_이상_랜덤_문자열
```

## 5. PostgreSQL / Redis 설정

현재 기본 설정은 로컬 DB와 Redis를 가리킨다.

운영 인프라 주소와 계정으로 변경한다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://운영DB호스트:5432/todaydev
    username: 운영DB계정
    password: 운영DB비밀번호

  r2dbc:
    url: r2dbc:postgresql://운영DB호스트:5432/todaydev
    username: 운영DB계정
    password: 운영DB비밀번호

  flyway:
    url: jdbc:postgresql://운영DB호스트:5432/todaydev
    user: 운영DB계정
    password: 운영DB비밀번호

  data:
    redis:
      host: 운영Redis호스트
      port: 6379
```

필수 확인:

- Flyway migration이 운영 DB에 정상 적용되어야 한다.
- `notification_preference`, `notification_delivery` 테이블이 생성되어야 한다.

## 6. RabbitMQ 설정

notification 큐 provider가 `rabbitmq`로 설정되어 있으므로 운영 RabbitMQ가 필요하다.

```env
RABBITMQ_HOST=운영_RabbitMQ_호스트
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=운영_RabbitMQ_계정
RABBITMQ_PASSWORD=운영_RabbitMQ_비밀번호
NOTIFICATION_RABBITMQ_EXCHANGE=notification.exchange
NOTIFICATION_RABBITMQ_REQUEST_QUEUE=notification.requested.queue
NOTIFICATION_RABBITMQ_RETRY_QUEUE=notification.retry.queue
NOTIFICATION_RABBITMQ_DLQ=notification.dlq
```

주의:

- 운영에서는 `guest/guest`를 사용하지 않는다.
- 애플리케이션 기동 시 durable exchange/queue가 자동 선언된다.
- request/retry/dlq queue 이름이 운영 RabbitMQ와 백엔드 설정에서 일치해야 한다.

## 7. SMTP / Email 설정

현재 기본 SMTP는 로컬 Mailpit 기준이다.

운영 메일 발송을 위해 실제 SMTP provider 설정으로 변경한다.

```env
EMAIL_NOTIFICATION_ENABLED=true
SMTP_HOST=운영_SMTP_호스트
SMTP_PORT=587
SMTP_USERNAME=운영_SMTP_사용자
SMTP_PASSWORD=운영_SMTP_비밀번호
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
SMTP_FROM=no-reply@운영도메인
```

추가 운영 작업:

- `SMTP_FROM`은 SMTP provider에서 허용된 발신 주소여야 한다.
- 운영 도메인에 SPF, DKIM, DMARC를 설정한다.
- 클라우드 환경에서 SMTP 포트 egress가 막혀 있지 않은지 확인한다.

## 8. 외부 API 키

브리핑 생성 자체가 외부 API와 AI 요약에 의존한다.

운영 키를 반드시 주입한다.

```env
GEMINI_API_KEY=운영_Gemini_API_Key
GITHUB_PAT=운영_GitHub_Token
```

주의:

- `GEMINI_API_KEY`가 없거나 제한되면 브리핑 요약이 실패할 수 있다.
- `GITHUB_PAT`가 없으면 GitHub API rate limit에 빨리 걸릴 수 있다.

## 9. Slack / Discord Webhook URL

Slack/Discord 실제 webhook URL은 백엔드 환경변수에 고정 설정되어 있지 않다.

운영 사용자가 프론트 알림 화면에서 직접 등록해야 한다.

채널별 destination:

- `EMAIL`: 이메일 주소
- `SLACK`: Slack Incoming Webhook 전체 URL
- `DISCORD`: Discord Webhook 전체 URL

예시:

```text
https://hooks.slack.com/services/...
https://discord.com/api/webhooks/...
```

주의:

- API 응답에는 destination 원문이 노출되지 않고 `configured` 여부만 반환된다.
- webhook URL이 삭제되었거나 만료되면 테스트 발송 또는 브리핑 알림 발송이 실패한다.

## 10. 권장 코드 보완

현재 Slack/Discord sender는 notification enabled 값을 확인하지만, Email sender는 `EMAIL_NOTIFICATION_ENABLED=false`를 확인하지 않고 SMTP 발송을 시도한다.

운영 전에 `SmtpEmailNotificationSender`에 아래 조건 추가를 권장한다.

대상 파일:

```text
src/main/java/com/todaydev/notification/infrastructure/email/SmtpEmailNotificationSender.java
```

권장 추가 코드:

```java
if (!properties.enabled() || !properties.email().enabled()) {
    return Mono.empty();
}
```

적용 위치:

```java
@Override
public Mono<Void> send(EmailNotificationPayload payload) {
    if (!properties.enabled() || !properties.email().enabled()) {
        return Mono.empty();
    }

    return Mono.fromRunnable(() -> sendBlocking(payload))
            .subscribeOn(Schedulers.boundedElastic())
            .then();
}
```

## 11. 배포 후 검증 순서

1. 백엔드 애플리케이션 기동 확인
2. Flyway migration 성공 확인
3. RabbitMQ management UI에서 아래 리소스 생성 확인
   - `notification.exchange`
   - `notification.requested.queue`
   - `notification.retry.queue`
   - `notification.dlq`
4. 프론트에서 로그인 확인
5. 알림 화면에서 EMAIL preference 저장 후 테스트 발송
6. 알림 화면에서 SLACK preference 저장 후 테스트 발송
7. 알림 화면에서 DISCORD preference 저장 후 테스트 발송
8. 새 브리핑 생성
9. 브리핑 생성 완료 후 알림 이력에서 channel별 상태 확인
10. `notification_delivery.status`가 `SENT`인지 확인

운영 확인 SQL:

```sql
SELECT channel, enabled, updated_at
FROM notification_preference
WHERE user_id = :user_id;

SELECT delivery_id, briefing_id, channel, status, attempt_count,
       last_error_code, last_error_message, queued_at, sent_at, updated_at
FROM notification_delivery
WHERE user_id = :user_id
ORDER BY created_at DESC;
```

## 12. 실패 시 우선 확인 항목

브리핑은 생성됐지만 알림 이력이 없음:

- 사용자 notification preference가 enabled 상태인지 확인
- `notification.enabled=true`인지 확인
- 브리핑 상태가 `FAILED`가 아닌지 확인
- 서버 로그에서 `Notification enqueue failed` 검색

delivery가 `FAILED`:

- RabbitMQ 연결 정보 확인
- exchange/queue 권한 확인
- `NOTIFICATION_PUBLISH_FAILED` 여부 확인

delivery가 `RETRYING` 또는 `DLQ`:

- SMTP 계정/비밀번호/STARTTLS 확인
- Slack/Discord webhook URL 유효성 확인
- 외부 네트워크 egress 확인
- 429 rate limit 여부 확인

Email만 실패:

- `SMTP_FROM`이 provider에서 허용된 주소인지 확인
- SPF/DKIM/DMARC 확인
- SMTP port가 막혀 있지 않은지 확인

Slack/Discord만 실패:

- webhook URL 전체가 저장되어 있는지 확인
- webhook이 삭제/만료되지 않았는지 확인
- 운영 서버에서 `hooks.slack.com`, `discord.com`으로 나갈 수 있는지 확인
