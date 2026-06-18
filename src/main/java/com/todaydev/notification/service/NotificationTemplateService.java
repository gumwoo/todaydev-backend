package com.todaydev.notification.service;

import com.todaydev.briefing.domain.Briefing;
import com.todaydev.briefing.domain.BriefingStatus;
import com.todaydev.briefing.repository.BriefingItemDetail;
import com.todaydev.common.config.properties.NotificationProperties;
import com.todaydev.notification.infrastructure.discord.DiscordWebhookPayload;
import com.todaydev.notification.infrastructure.email.EmailNotificationPayload;
import com.todaydev.notification.infrastructure.slack.SlackWebhookPayload;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationProperties properties;

    public NotificationTemplateService(NotificationProperties properties) {
        this.properties = properties;
    }

    public EmailNotificationPayload createBriefingEmail(String to, Briefing briefing, List<BriefingItemDetail> items) {
        String subject = "%s 오늘의 개발 브리핑이 도착했어요".formatted(properties.email().subjectPrefix());
        return new EmailNotificationPayload(to, subject, renderBriefingHtml(briefing, items));
    }

    public SlackWebhookPayload createBriefingSlackPayload(Briefing briefing, List<BriefingItemDetail> items) {
        return new SlackWebhookPayload(renderBriefingText(briefing, items));
    }

    public DiscordWebhookPayload createBriefingDiscordPayload(Briefing briefing, List<BriefingItemDetail> items) {
        return new DiscordWebhookPayload(renderBriefingText(briefing, items));
    }

    public String createTestEmailSubject() {
        return "%s 알림 테스트".formatted(properties.email().subjectPrefix());
    }

    public String createTestEmailBody() {
        return """
                <!doctype html>
                <html lang="ko">
                <body style="font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#172033;">
                  <h1 style="font-size:20px;">Todaydev 알림 테스트</h1>
                  <p>알림 설정이 정상적으로 연결되었습니다.</p>
                </body>
                </html>
                """;
    }

    public SlackWebhookPayload createTestSlackPayload() {
        return new SlackWebhookPayload("Todaydev 알림 테스트: Slack 알림 설정이 정상적으로 연결되었습니다.");
    }

    public DiscordWebhookPayload createTestDiscordPayload() {
        return new DiscordWebhookPayload("Todaydev 알림 테스트: Discord 알림 설정이 정상적으로 연결되었습니다.");
    }

    private String renderBriefingHtml(Briefing briefing, List<BriefingItemDetail> items) {
        List<BriefingItemDetail> topItems = items.stream()
                .sorted(Comparator.comparing(BriefingItemDetail::score).reversed())
                .limit(properties.maxItemsPerMessage())
                .toList();

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Todaydev Briefing</title>
                </head>
                <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f7fb;padding:28px 0;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="width:640px;max-width:100%%;background:#ffffff;border:1px solid #e5eaf2;border-radius:8px;overflow:hidden;">
                          <tr>
                            <td style="padding:28px 32px 20px;background:#172033;color:#ffffff;">
                              <div style="font-size:13px;color:#a9b7d0;margin-bottom:8px;">Todaydev Briefing</div>
                              <h1 style="margin:0;font-size:24px;line-height:1.35;font-weight:700;">오늘의 개발 브리핑</h1>
                              <div style="margin-top:10px;font-size:14px;color:#d7deeb;">%s · %s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:28px 32px 12px;">
                              <h2 style="margin:0 0 10px;font-size:18px;line-height:1.4;color:#172033;">요약</h2>
                              <p style="margin:0;font-size:15px;line-height:1.75;color:#34425a;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:8px 32px 28px;">
                              <h2 style="margin:0 0 14px;font-size:18px;line-height:1.4;color:#172033;">추천 항목</h2>
                              %s
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escape(statusLabel(briefing.status())),
                escape(briefing.generatedAt().format(DATE_TIME_FORMATTER)),
                escape(summary(briefing)),
                renderItems(topItems)
        );
    }

    private String renderItems(List<BriefingItemDetail> items) {
        if (items.isEmpty()) {
            return """
                    <div style="padding:18px;border:1px solid #e5eaf2;border-radius:8px;background:#f8fafc;color:#5f6f89;font-size:14px;line-height:1.6;">
                      이번 브리핑에 포함된 추천 항목이 없습니다.
                    </div>
                    """;
        }

        StringBuilder builder = new StringBuilder();
        for (BriefingItemDetail item : items) {
            builder.append("""
                    <div style="padding:18px 0;border-top:1px solid #e5eaf2;">
                      <div style="font-size:12px;font-weight:700;color:#4f6bed;text-transform:uppercase;">%s · score %s</div>
                      <a href="%s" style="display:block;margin-top:7px;font-size:17px;line-height:1.45;font-weight:700;color:#172033;text-decoration:none;">%s</a>
                      <p style="margin:8px 0 0;font-size:14px;line-height:1.65;color:#4b5c75;">%s</p>
                    </div>
                    """.formatted(
                    escape(item.source().name()),
                    escape(item.score().stripTrailingZeros().toPlainString()),
                    escape(item.url()),
                    escape(item.title()),
                    escape(item.summary() == null || item.summary().isBlank() ? "요약 정보가 없습니다." : item.summary())
            ));
        }
        return builder.toString();
    }

    private String renderBriefingText(Briefing briefing, List<BriefingItemDetail> items) {
        List<BriefingItemDetail> topItems = items.stream()
                .sorted(Comparator.comparing(BriefingItemDetail::score).reversed())
                .limit(properties.maxItemsPerMessage())
                .toList();

        StringBuilder builder = new StringBuilder();
        builder.append("Todaydev 오늘의 개발 브리핑\n");
        builder.append(statusLabel(briefing.status()))
                .append(" · ")
                .append(briefing.generatedAt().format(DATE_TIME_FORMATTER))
                .append("\n\n");
        builder.append(summary(briefing)).append("\n");

        if (topItems.isEmpty()) {
            builder.append("\n추천 항목이 없습니다.");
            return builder.toString();
        }

        builder.append("\n추천 항목\n");
        for (int index = 0; index < topItems.size(); index++) {
            BriefingItemDetail item = topItems.get(index);
            builder.append(index + 1)
                    .append(". ")
                    .append(item.title())
                    .append(" (")
                    .append(item.source().name())
                    .append(")\n")
                    .append(item.url())
                    .append("\n");
        }
        return builder.toString();
    }

    private String summary(Briefing briefing) {
        if (briefing.summary() == null || briefing.summary().isBlank()) {
            return "브리핑 요약이 아직 준비되지 않았습니다. 아래 추천 항목을 먼저 확인해 주세요.";
        }
        return briefing.summary();
    }

    private String statusLabel(BriefingStatus status) {
        return switch (status) {
            case COMPLETED -> "완료";
            case PARTIAL -> "일부 완료";
            case SUMMARY_FAILED -> "요약 실패";
            case FAILED -> "실패";
            case GENERATING -> "생성 중";
        };
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
