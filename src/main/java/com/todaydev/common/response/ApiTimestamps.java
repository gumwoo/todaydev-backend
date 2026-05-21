package com.todaydev.common.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

final class ApiTimestamps {

    private static final ZoneId API_ZONE = ZoneId.of("Asia/Seoul");

    private ApiTimestamps() {
    }

    static String now() {
        return OffsetDateTime.now(API_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
