# Notification Delivery Design

이 문서는 기존 WebFlux 구조를 유지하면서 브리핑 결과를 Email, Slack, Discord로 발송하는 알림/다이제스트 기능을 추가하기 위한 설계 문서입니다.

목표는 단순 외부 Webhook 호출이 아니라 메시지 큐, 비동기 소비, 재시도, DLQ를 통해 포트폴리오에서 설명 가능한 장애 대응 구조를 만드는 것입니다.

## 1. 설계 원칙

- `API_CONTRACT.md`, `CODING_RULES.md`, `TESTING_RULES.md`, `DONE_DEFINITION.md`를 우선한다.
- Controller는 얇게 유지하고, 발송 유스케이스는 Service/Application 계층에 둔다.
- 브리핑 생성 파이프라인은 발송 채널 장애에 의해 실패하지 않는다.
- WebFlux 흐름을 유지하고 business service에서 `.block()`, 직접 `.subscribe()`를 사용하지 않는다.
- 메시지 큐 발행/소비의 subscription boundary는 `notification/infrastructure`에만 둔다.
- Email, Slack, Discord secret과 webhook URL은 DB/설정에서 관리하되 로그와 응답에 노출하지 않는다.
- 큐 구현체는 Kafka 또는 RabbitMQ 중 하나를 선택할 수 있도록 내부 port를 먼저 정의한다.
- 재시도 횟수, backoff, DLQ topic/queue 이름, consumer concurrency는 `application.yml`과 `@ConfigurationProperties`로 중앙 관리한다.

## 2. 권장 전체 흐름

```text
정기 스케줄 또는 수동 브리핑 생성
-> briefing status COMPLETED / PARTIAL / SUMMARY_FAILED
-> NotificationEnqueueService가 사용자 발송 설정 조회
-> 채널별 NotificationMessage 생성
-> NotificationPublisher가 Kafka topic 또는 RabbitMQ exchange에 발행
-> NotificationConsumer가 비동기 소비
-> Email / Slack / Discord sender 호출
-> notification_delivery 저장
-> 실패 시 retry
-> 최종 실패 시 DLQ 이동 및 notification_delivery FAILED 기록
```

브리핑 생성과 알림 발송의 경계:

- 브리핑 생성 완료 여부는 `briefing.status`로 결정한다.
- 알림 발송 실패는 브리핑 상태를 변경하지 않는다.
- 발송 상태는 `notification_delivery`에서 별도로 추적한다.

## 3. 패키지 추가 구조

```text
com.todaydev
├─ notification
│  ├─ domain
│  ├─ service
│  ├─ repository
│  ├─ infrastructure
│  │  ├─ queue
│  │  ├─ email
│  │  ├─ slack
│  │  └─ discord
│  └─ web
└─ common
   └─ config
      └─ properties
```

기존 패키지는 아래처럼 최소 변경한다.

```text
briefing
└─ service
   └─ BriefingGenerationService

schedule
└─ service
   └─ ScheduledBriefingService

common
├─ exception
└─ config
```

## 4. `notification/domain`

알림 도메인의 순수 규칙을 둔다. Spring Web, WebClient, queue client에 의존하지 않는다.

추가 파일:

- `NotificationChannel`
- `NotificationDeliveryStatus`
- `NotificationPreference`
- `NotificationDelivery`
- `NotificationMessage`
- `NotificationFailureReason`

권장 enum:

```text
NotificationChannel:
EMAIL
SLACK
DISCORD

NotificationDeliveryStatus:
PENDING
PUBLISHED
SENDING
SENT
RETRYING
FAILED
DLQ
SKIPPED
```

도메인 규칙:

- 채널별 수신 정보는 `NotificationPreference`가 검증한다.
- Email 주소는 Bean Validation 대상이며, 도메인에서도 빈 값 발송을 막는다.
- Slack/Discord webhook URL은 응답 DTO와 로그에 직접 노출하지 않는다.
- 같은 `briefingId`, `userId`, `channel` 조합은 중복 발송되지 않도록 유니크 정책을 둔다.
- `SUMMARY_FAILED` 브리핑은 발송할 수 있지만, 제목 또는 본문에 AI 요약 실패 fallback 문구를 사용한다.
- `FAILED` 브리핑은 발송하지 않고 `SKIPPED`로 기록한다.

## 5. `notification/service`

알림 유스케이스 흐름을 조합한다.

추가 파일:

- `NotificationPreferenceService`
- `NotificationEnqueueService`
- `NotificationDeliveryService`
- `NotificationMessageFactory`
- `NotificationTemplateService`
- `NotificationRetryPolicy`

책임:

- `NotificationPreferenceService`
  - 사용자별 Email/Slack/Discord 발송 설정 CRUD를 담당한다.
  - Controller에서 전달받은 authenticated user 기준으로만 조회/수정한다.

- `NotificationEnqueueService`
  - 브리핑 완료 후 발송 대상 채널을 조회한다.
  - 채널별 `notification_delivery` row를 `PENDING`으로 만든다.
  - `NotificationPublisher`에 메시지 발행을 요청한다.
  - 발행 성공 시 `PUBLISHED`, 발행 실패 시 `FAILED` 또는 retry 가능한 상태로 저장한다.

- `NotificationDeliveryService`
  - consumer가 메시지를 받은 뒤 발송 상태를 `SENDING`, `SENT`, `RETRYING`, `FAILED`, `DLQ`로 전이한다.
  - 외부 발송 adapter의 결과를 도메인 상태로 변환한다.

- `NotificationMessageFactory`
  - `briefing`, `briefing_item`, 사용자 정보, 채널 설정을 조합해 큐 메시지를 만든다.
  - 메시지에는 webhook secret 원문을 넣지 않는 것을 우선한다.
  - Slack/Discord는 `preferenceId` 또는 암호화된 destination reference만 넣고 consumer가 DB에서 조회한다.

- `NotificationTemplateService`
  - Email subject/body, Slack payload, Discord payload를 만든다.
  - 템플릿 내용은 채널별로 분리한다.
  - 브리핑 항목 개수 제한, 본문 최대 길이는 설정값으로 관리한다.

Reactive 규칙:

- 모든 service public method는 `Mono<T>` 또는 `Flux<T>`를 반환한다.
- queue client가 blocking SDK만 제공하면 `infrastructure`에서 `Schedulers.boundedElastic()`로 격리한다.
- service 내부에서 직접 `.subscribe()`하지 않는다.

## 6. `notification/repository`

R2DBC 기반 저장/조회만 담당한다.

추가 파일:

- `NotificationPreferenceRepository`
- `R2dbcNotificationPreferenceRepository`
- `NotificationDeliveryRepository`
- `R2dbcNotificationDeliveryRepository`

필요 쿼리:

- 사용자 알림 설정 조회
- 사용자 알림 설정 upsert
- 활성 채널 목록 조회
- 발송 row 생성
- 발송 상태 전이
- `briefingId`, `userId`, `channel` 기준 중복 확인
- DLQ 또는 실패 발송 목록 조회

Repository 규칙:

- 사용자 입력으로 SQL 문자열을 조합하지 않는다.
- webhook URL, email destination은 응답용 DTO에 그대로 싣지 않는다.
- 상태 전이는 낙관적 갱신을 고려한다. 예: 현재 상태가 `PUBLISHED`일 때만 `SENDING`으로 변경.

## 7. `notification/infrastructure/queue`

Kafka/RabbitMQ 구현체를 숨기는 큐 adapter 계층이다.

추가 interface:

- `NotificationPublisher`
- `NotificationConsumer`
- `NotificationDeadLetterPublisher`

추가 DTO:

- `NotificationQueueMessage`
- `NotificationQueueHeaders`

메시지 필드:

```json
{
  "messageId": "01J...",
  "deliveryId": 1,
  "briefingId": 10,
  "userId": 3,
  "channel": "SLACK",
  "attempt": 1,
  "createdAt": "2026-06-16T09:00:00+09:00",
  "traceId": "01J..."
}
```

메시지 규칙:

- idempotency 기준은 `deliveryId`를 사용한다.
- 큐 메시지에는 access token, refresh token, stream token, API key, webhook URL 원문을 넣지 않는다.
- `traceId`를 header 또는 payload에 포함해 발행부터 소비까지 추적한다.
- payload schema version을 header로 관리한다. 예: `notification-message-version: 1`

### Kafka 선택 시

권장 dependency:

```gradle
implementation 'io.projectreactor.kafka:reactor-kafka'
```

권장 topic:

```text
todaydev.notification.requested
todaydev.notification.retry
todaydev.notification.dlq
```

구현 파일:

- `KafkaNotificationPublisher`
- `KafkaNotificationConsumer`
- `KafkaNotificationDeadLetterPublisher`
- `KafkaNotificationConfig`

특징:

- consumer group으로 수평 확장이 쉽다.
- 메시지 ordering과 offset 관리 설명이 가능하다.
- delayed retry는 별도 retry topic과 scheduled republish 전략이 필요하다.

### RabbitMQ 선택 시

권장 dependency:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-amqp'
```

권장 exchange/queue:

```text
notification.exchange
notification.requested.queue
notification.retry.queue
notification.dlq
```

구현 파일:

- `RabbitNotificationPublisher`
- `RabbitNotificationConsumer`
- `RabbitNotificationDeadLetterPublisher`
- `RabbitNotificationConfig`

특징:

- DLX, TTL 기반 delayed retry 구성이 직관적이다.
- 작업 큐와 재시도/DLQ를 포트폴리오에서 설명하기 쉽다.
- Spring AMQP가 blocking 기반이면 boundedElastic 격리 또는 별도 listener thread boundary를 명확히 문서화한다.

선택 권장:

- 현재 프로젝트가 단일 서비스 포트폴리오라면 RabbitMQ가 retry/DLQ 시연이 단순하다.
- 이벤트 스트림, consumer group, offset 기반 재처리를 강조하려면 Kafka를 선택한다.
- 코드 구조는 `NotificationPublisher` port를 기준으로 두어 둘 중 하나만 활성화한다.

## 8. `notification/infrastructure/email`

Email 발송 adapter를 둔다.

추가 파일:

- `EmailNotificationSender`
- `EmailNotificationPayload`
- `EmailClientConfig`

권장 방식:

- 운영은 SMTP 또는 SendGrid/Mailgun 같은 provider adapter로 분리한다.
- 로컬 개발은 console 또는 fake sender profile을 둔다.
- blocking SMTP client를 쓰면 boundedElastic로 격리한다.

설정 예시:

```yaml
notification:
  email:
    enabled: false
    from: no-reply@todaydev.local
    timeout-millis: 3000
```

## 9. `notification/infrastructure/slack`

Slack Webhook 발송 adapter를 둔다.

추가 파일:

- `SlackNotificationSender`
- `SlackWebhookPayload`
- `SlackClientConfig`

규칙:

- WebClient를 사용한다.
- webhook URL은 `NotificationPreferenceRepository`에서 조회한다.
- timeout, retry는 sender 내부에서 무한 반복하지 않고 consumer retry 정책에 위임한다.
- Slack API 응답의 raw body를 그대로 사용자 응답에 전달하지 않는다.

## 10. `notification/infrastructure/discord`

Discord Webhook 발송 adapter를 둔다.

추가 파일:

- `DiscordNotificationSender`
- `DiscordWebhookPayload`
- `DiscordClientConfig`

규칙:

- WebClient를 사용한다.
- Discord embed 길이 제한을 template에서 관리한다.
- 429 rate limit 응답은 retry 가능한 실패로 분류한다.
- webhook URL과 raw error body는 로그에 마스킹한다.

## 11. `notification/web`

사용자 알림 설정과 발송 이력 조회 API를 둔다.

추가 파일:

- `NotificationPreferenceController`
- `NotificationPreferenceRequest`
- `NotificationPreferenceResponse`
- `NotificationDeliveryResponse`
- `NotificationDeliveriesResponse`
- `TestNotificationRequest`

권장 API:

```text
GET    /api/notifications/me/preferences
PUT    /api/notifications/me/preferences/{channel}
DELETE /api/notifications/me/preferences/{channel}
GET    /api/notifications/me/deliveries?page=0&size=20
POST   /api/notifications/me/test
```

API 계약 추가 필요:

- `API_CONTRACT.md`에 요청/응답 DTO, HTTP status, ErrorCode를 추가한다.
- 응답은 공통 `ApiResponse` 포맷을 따른다.
- webhook URL은 등록 요청에만 받고 응답에는 `configured: true`처럼 마스킹된 상태만 반환한다.

## 12. `common/config/properties`

설정 중앙화를 위해 properties를 추가한다.

추가 파일:

- `NotificationProperties`
- `QueueProperties`

설정 예시:

```yaml
notification:
  enabled: true
  max-items-per-message: 5
  queue:
    provider: rabbitmq
    publish-timeout-millis: 3000
    consumer-concurrency: 4
    retry-max-attempts: 3
    retry-backoff-millis: 5000
    dlq-enabled: true
  email:
    enabled: false
    from: no-reply@todaydev.local
    timeout-millis: 3000
  slack:
    enabled: true
    timeout-millis: 3000
  discord:
    enabled: true
    timeout-millis: 3000
```

Kafka 설정 예시:

```yaml
notification:
  queue:
    provider: kafka
    kafka:
      bootstrap-servers: localhost:9092
      request-topic: todaydev.notification.requested
      retry-topic: todaydev.notification.retry
      dlq-topic: todaydev.notification.dlq
      consumer-group-id: todaydev-notification
```

RabbitMQ 설정 예시:

```yaml
notification:
  queue:
    provider: rabbitmq
    rabbitmq:
      host: localhost
      port: 5672
      exchange: notification.exchange
      request-queue: notification.requested.queue
      retry-queue: notification.retry.queue
      dlq: notification.dlq
```

## 13. `common/exception`

알림 기능 ErrorCode를 추가한다.

추가 ErrorCode:

```text
NOTIFICATION_PREFERENCE_NOT_FOUND
NOTIFICATION_CHANNEL_UNSUPPORTED
NOTIFICATION_DESTINATION_INVALID
NOTIFICATION_DELIVERY_NOT_FOUND
NOTIFICATION_PUBLISH_FAILED
NOTIFICATION_SEND_FAILED
NOTIFICATION_RATE_LIMITED
NOTIFICATION_DLQ_PUBLISHED
```

HTTP status 기준:

- 잘못된 채널 또는 destination: `400`
- 본인 리소스가 아닌 발송 이력 접근: `404`
- 중복 설정 정책 위반: `409`
- 외부 채널 rate limit: 내부 소비에서는 retry, API 응답으로 노출될 경우 `429`
- 큐 발행 실패: `503`
- 채널 발송 실패: 내부 소비에서는 retry/DLQ, test API에서는 `502` 또는 `503`

## 14. DB 마이그레이션

추가 migration 예시:

```text
V4__add_notification_tables.sql
```

테이블:

```sql
CREATE TABLE notification_preference (
    preference_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    destination VARCHAR(2000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_preference UNIQUE (user_id, channel)
);

CREATE TABLE notification_delivery (
    delivery_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    briefing_id BIGINT NOT NULL REFERENCES briefing(briefing_id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(100),
    last_error_message TEXT,
    queued_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_delivery UNIQUE (user_id, briefing_id, channel)
);

CREATE INDEX idx_notification_delivery_user_created
    ON notification_delivery (user_id, created_at DESC);

CREATE INDEX idx_notification_delivery_status
    ON notification_delivery (status, updated_at);
```

보안 보완:

- `destination`은 MVP에서는 평문 저장 가능하지만 운영 설계에서는 암호화 대상이다.
- 로그와 API 응답에는 `destination` 원문을 노출하지 않는다.
- 암호화까지 구현할 경우 `common/security` 또는 별도 `common/crypto`에 encryption adapter를 둔다.

## 15. 기존 패키지 변경 지점

### `briefing/service`

변경 대상:

- `BriefingGenerationService`

추가 흐름:

- 브리핑 저장 완료 후 `NotificationEnqueueService.enqueueForBriefing(briefingId)` 호출
- 호출 실패는 브리핑 생성 실패로 전파하지 않는다.
- 발송 요청 실패는 로그와 `notification_delivery` 상태로 추적한다.

주의:

- service 내부에서 직접 `.subscribe()`하지 않는다.
- `onErrorResume`으로 알림 큐 발행 실패를 격리하되, 빈 catch처럼 삼키지 말고 traceId와 briefingId를 남긴다.

### `schedule/service`

변경 대상:

- `ScheduledBriefingService`

원칙:

- 정기 스케줄은 계속 브리핑 생성을 트리거한다.
- 알림 발송 여부는 `NotificationPreference`가 결정한다.
- 스케줄러가 Email/Slack/Discord sender를 직접 호출하지 않는다.

### `docker-compose.yml`

Kafka 선택 시:

- Kafka broker와 필요한 경우 UI를 추가한다.
- local bootstrap server를 `application.yml`에 연결한다.

RabbitMQ 선택 시:

- RabbitMQ management image를 추가한다.
- management UI 포트는 로컬 확인용으로만 문서화한다.

권장 MVP:

- RabbitMQ를 먼저 붙여 retry/DLQ를 빠르게 시연한다.
- Kafka는 별도 branch 또는 확장 문서에서 provider 교체 사례로 남긴다.

## 16. 테스트 추가 범위

단위 테스트:

- `NotificationMessageFactoryTest`
- `NotificationTemplateServiceTest`
- `NotificationRetryPolicyTest`
- `NotificationDeliveryServiceTest`
- `NotificationPreferenceServiceTest`

WebFlux/API 테스트:

- `NotificationPreferenceControllerTest`
- `NotificationDeliveryControllerTest`

Adapter 테스트:

- Slack WebClient timeout, 429, 500
- Discord WebClient timeout, 429, 500
- Email fake sender success/failure
- Kafka 또는 RabbitMQ publisher failure
- consumer retry 후 success
- retry 초과 후 DLQ

Repository 테스트:

- preference upsert
- delivery unique constraint
- status transition
- userId 기준 접근 제한

검증 명령:

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
rg -n "\.block\(|\.blockOptional\(|\.subscribe\(" src/main/java src/test/java
```

허용 기준:

- `.subscribe()`는 기존 `briefing/infrastructure/BriefingJobDispatcher.java`와 새 `notification/infrastructure` consumer boundary에만 허용한다.
- blocking Email 또는 AMQP client는 boundedElastic 또는 listener container boundary로 격리한다.
- 외부 Slack/Discord/Email 테스트는 실제 네트워크에 의존하지 않는다.

## 17. API 계약 문서 추가 항목

`API_CONTRACT.md`에 아래 내용을 추가한다.

- Notification channel enum
- Notification delivery status enum
- Notification ErrorCode
- 알림 설정 API
- 발송 이력 API
- 테스트 발송 API

응답 예시:

```json
{
  "success": true,
  "data": {
    "channel": "SLACK",
    "enabled": true,
    "configured": true,
    "updatedAt": "2026-06-16T09:00:00+09:00"
  },
  "timestamp": "2026-06-16T09:00:00+09:00"
}
```

## 18. 운영/관측 포인트

로그:

- `traceId`
- `deliveryId`
- `briefingId`
- `channel`
- `attempt`
- `status`
- `latencyMs`

로그 금지:

- webhook URL
- email full address가 민감하다고 판단되는 경우 원문
- access token
- refresh token
- stream token
- API key
- raw Authorization header

지표:

- 발행 성공/실패 수
- 채널별 발송 성공/실패 수
- retry count
- DLQ count
- p95 발송 latency
- queue lag 또는 ready message count

## 19. 구현 순서

1. `API_CONTRACT.md`에 Notification 계약을 먼저 추가한다.
2. `NotificationProperties`, `QueueProperties`를 추가하고 `application.yml` 기본값을 정한다.
3. DB migration으로 preference/delivery 테이블을 추가한다.
4. `notification/domain`, `repository`, `service`를 큐 없이 먼저 구현한다.
5. `notification/web`으로 설정 CRUD와 이력 조회를 구현한다.
6. `NotificationPublisher` port와 fake/in-memory adapter를 붙여 service 테스트를 안정화한다.
7. RabbitMQ 또는 Kafka provider 중 하나를 선택해 `notification/infrastructure/queue` 구현체를 추가한다.
8. Slack/Discord WebClient sender를 구현하고 timeout/failure 테스트를 추가한다.
9. Email sender는 fake sender부터 시작하고, 실제 SMTP/provider는 설정 기반으로 확장한다.
10. `BriefingGenerationService`의 완료 흐름에 `NotificationEnqueueService`를 연결한다.
11. retry/DLQ 동작을 통합 테스트 또는 로컬 docker compose로 검증한다.
12. `README.md`, `PERFORMANCE_VALIDATION.md`에 실행 방법과 검증 기준을 추가한다.

## 20. 포트폴리오 어필 포인트

- 브리핑 생성과 발송을 메시지 큐로 분리해 외부 채널 장애가 핵심 기능에 전파되지 않는다.
- Kafka/RabbitMQ adapter를 port로 추상화해 큐 구현체 교체 가능성을 보여준다.
- consumer retry와 DLQ로 일시 장애와 영구 실패를 구분한다.
- WebFlux 서비스 계층은 reactive 흐름을 유지하고 subscription boundary를 infrastructure에 격리한다.
- channel adapter를 Email/Slack/Discord로 분리해 외부 연동 책임을 명확히 나눈다.
- `notification_delivery`로 발송 상태를 영속화해 장애 분석과 재처리가 가능하다.
