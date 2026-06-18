# Notification Deployment Guide

## 1. 배포 흐름

```text
1. 코드 빌드
   └─ sh ./gradlew test

2. 인프라 준비
   ├─ PostgreSQL
   ├─ Redis
   ├─ RabbitMQ
   └─ SMTP provider

3. 환경변수 주입
   ├─ DB / Redis / RabbitMQ 연결 정보
   ├─ SMTP 계정과 발신 주소
   ├─ JWT_SECRET
   ├─ GEMINI_API_KEY / GITHUB_PAT
   └─ CORS / refresh cookie 운영값

4. DB migration 실행
   └─ Flyway가 notification_preference, notification_delivery 테이블 생성

5. 애플리케이션 배포
   ├─ notification.queue.provider=rabbitmq
   ├─ RabbitMQ exchange/queue 자동 선언
   └─ request queue listener 기동

6. 배포 후 검증
   ├─ /actuator 또는 서버 로그로 기동 확인
   ├─ RabbitMQ management UI에서 exchange/queue 생성 확인
   ├─ EMAIL preference 등록 후 /api/notifications/me/test 호출
   ├─ SLACK/DISCORD webhook preference 등록 후 test 호출
   └─ 브리핑 생성 후 notification_delivery 상태가 SENT인지 확인
```

## 2. 배포 후 바꿔야 하는 설정

### 보안

```text
JWT_SECRET=운영용 32자 이상 랜덤 문자열
app.auth.refresh-cookie.secure=true
app.auth.refresh-cookie.same-site=None 또는 운영 프론트 정책에 맞는 값
app.cors.allowed-origins=https://운영-프론트-도메인
```

- Slack/Discord webhook URL은 API 요청 body로만 받고 응답에는 노출하지 않는다.
- 운영 로그에 webhook URL, JWT, API key가 찍히지 않도록 로그 레벨과 필터를 확인한다.

### RabbitMQ

```text
RABBITMQ_HOST=운영 RabbitMQ host
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=운영 계정
RABBITMQ_PASSWORD=운영 비밀번호
NOTIFICATION_RABBITMQ_EXCHANGE=notification.exchange
NOTIFICATION_RABBITMQ_REQUEST_QUEUE=notification.requested.queue
NOTIFICATION_RABBITMQ_RETRY_QUEUE=notification.retry.queue
NOTIFICATION_RABBITMQ_DLQ=notification.dlq
```

- 운영에서는 `guest/guest`를 사용하지 않는다.
- request/retry/dlq queue는 durable로 선언된다.
- retry queue는 TTL 후 request queue로 되돌아간다.

### SMTP / Email

```text
EMAIL_NOTIFICATION_ENABLED=true
SMTP_HOST=운영 SMTP host
SMTP_PORT=587
SMTP_USERNAME=운영 SMTP 사용자
SMTP_PASSWORD=운영 SMTP 비밀번호
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
SMTP_FROM=no-reply@운영도메인
```

- 운영 도메인의 SPF, DKIM, DMARC를 설정한다.
- 발신자 주소는 SMTP provider에서 허용된 주소로 맞춘다.

### 외부 API

```text
GEMINI_API_KEY=운영 키
GITHUB_PAT=운영 GitHub token
```

- rate limit이 낮은 키는 브리핑 생성과 알림 검증이 동시에 실패처럼 보일 수 있다.

### Notification 동작값

```text
notification.enabled=true
notification.max-items-per-message=5
notification.queue.retry-max-attempts=3
notification.queue.retry-backoff-millis=5000
notification.queue.consumer-concurrency=4
notification.queue.dlq-enabled=true
```

- 트래픽이 늘면 `consumer-concurrency`를 먼저 조정한다.
- 외부 webhook rate limit이 잦으면 concurrency를 낮추고 backoff를 늘린다.

## 3. 트러블슈팅

### API 설정 등록 실패

증상:

```text
NOTIFICATION_DESTINATION_INVALID
```

확인:

- `EMAIL`은 이메일 형식이어야 한다.
- `SLACK`, `DISCORD`는 `http` 또는 `https` webhook URL이어야 한다.
- URL 앞뒤 공백이 있는지 확인한다.

### 테스트 알림 실패

증상:

```text
NOTIFICATION_PREFERENCE_NOT_FOUND
NOTIFICATION_SEND_FAILED
NOTIFICATION_RATE_LIMITED
```

확인:

- 해당 channel preference가 등록되어 있고 `enabled=true`인지 확인한다.
- Slack/Discord webhook URL이 삭제되었거나 만료되지 않았는지 확인한다.
- SMTP 계정, 비밀번호, STARTTLS 설정을 확인한다.
- 외부 서비스 429가 나오면 retry backoff와 consumer concurrency를 조정한다.

### 브리핑은 생성됐지만 알림 이력이 없음

확인:

- 사용자의 notification preference가 하나 이상 enabled 상태인지 확인한다.
- `notification.enabled=true`인지 확인한다.
- 브리핑 상태가 `FAILED`이면 알림을 발행하지 않는다.
- 애플리케이션 로그에서 `Notification enqueue failed`를 검색한다.

### delivery가 FAILED 상태

확인:

- RabbitMQ 연결 정보가 맞는지 확인한다.
- exchange/queue가 RabbitMQ management UI에 생성되었는지 확인한다.
- `NOTIFICATION_PUBLISH_FAILED`가 기록되어 있으면 publisher 연결이나 권한 문제일 가능성이 높다.

### delivery가 RETRYING 또는 DLQ 상태

확인:

- `attempt_count`와 `last_error_code`를 확인한다.
- retry queue에 메시지가 쌓였다면 TTL 이후 request queue로 돌아오는지 확인한다.
- DLQ에 쌓였다면 webhook URL, SMTP, 외부 rate limit, 네트워크 egress를 확인한다.

### RabbitMQ listener가 메시지를 소비하지 않음

확인:

- `notification.queue.provider=rabbitmq`인지 확인한다.
- `spring.rabbitmq.*` 설정과 `notification.queue.rabbitmq.*` 설정이 같은 RabbitMQ를 가리키는지 확인한다.
- request queue 이름이 listener placeholder와 일치하는지 확인한다.
- consumer 로그에 JSON 역직렬화 오류가 있는지 확인한다.

### Email만 발송되지 않음

확인:

- `EMAIL_NOTIFICATION_ENABLED=true`인지 확인한다.
- `SMTP_FROM`이 SMTP provider에서 허용된 주소인지 확인한다.
- 587 포트는 STARTTLS가 필요한 경우가 많다.
- 로컬에서는 Mailpit `http://localhost:8025`에서 수신 여부를 확인한다.

### Slack/Discord만 발송되지 않음

확인:

- webhook URL 전체를 destination으로 저장했는지 확인한다.
- Slack app incoming webhook이 활성화되어 있는지 확인한다.
- Discord webhook이 삭제되었거나 채널 권한이 바뀌지 않았는지 확인한다.
- 운영 서버에서 `hooks.slack.com`, `discord.com`으로 egress가 가능한지 확인한다.

## 4. 운영 확인 SQL

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
