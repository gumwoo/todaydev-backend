package com.todaydev.auth.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaydev.auth.domain.User;
import com.todaydev.common.config.properties.JwtProperties;
import com.todaydev.common.exception.ErrorCode;
import com.todaydev.common.exception.TodaydevException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    @Autowired
    public JwtProvider(JwtProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    JwtProvider(JwtProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String createAccessToken(User user) {
        return createToken(user, TOKEN_TYPE_ACCESS, properties.accessTokenExpiry());
    }

    public String createRefreshToken(User user) {
        return createToken(user, TOKEN_TYPE_REFRESH, properties.refreshTokenExpiry());
    }

    public JwtClaims parseAccessToken(String token) {
        return parseToken(token, TOKEN_TYPE_ACCESS);
    }

    public JwtClaims parseRefreshToken(String token) {
        return parseToken(token, TOKEN_TYPE_REFRESH);
    }

    public long accessTokenExpiresIn() {
        return properties.accessTokenExpiry();
    }

    public long refreshTokenExpiresIn() {
        return properties.refreshTokenExpiry();
    }

    private String createToken(User user, String tokenType, long expiresInSeconds) {
        Instant now = Instant.now(clock);
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(user.userId()));
        payload.put("email", user.email());
        payload.put("typ", tokenType);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plusSeconds(expiresInSeconds).getEpochSecond());

        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signaturePart = sign(headerPart + "." + payloadPart);
        return headerPart + "." + payloadPart + "." + signaturePart;
    }

    private JwtClaims parseToken(String token, String expectedType) {
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new TodaydevException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new TodaydevException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        if (!expectedType.equals(payload.get("typ"))) {
            throw new TodaydevException(ErrorCode.AUTH_TOKEN_INVALID);
        }

        Instant expiresAt = Instant.ofEpochSecond(asLong(payload.get("exp")));
        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw new TodaydevException(ErrorCode.AUTH_TOKEN_EXPIRED);
        }

        return new JwtClaims(
                Long.valueOf(String.valueOf(payload.get("sub"))),
                String.valueOf(payload.get("email")),
                expiresAt
        );
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return encoder.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create JWT payload.", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(decoder.decode(value), MAP_TYPE);
        } catch (Exception exception) {
            throw new TodaydevException(ErrorCode.AUTH_TOKEN_INVALID);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return encoder.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT.", exception);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
        byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
        if (leftBytes.length != rightBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < leftBytes.length; i++) {
            result |= leftBytes[i] ^ rightBytes[i];
        }
        return result == 0;
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
