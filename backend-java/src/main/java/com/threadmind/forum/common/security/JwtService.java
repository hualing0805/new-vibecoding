package com.threadmind.forum.common.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] signingKey;
    private final long tokenTtlSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${threadmind.security.jwt-secret}") String jwtSecret,
            @Value("${threadmind.security.token-ttl-seconds:86400}") long tokenTtlSeconds
    ) {
        this(objectMapper, Clock.systemUTC(), jwtSecret, tokenTtlSeconds);
    }

    JwtService(ObjectMapper objectMapper, Clock clock, String jwtSecret, long tokenTtlSeconds) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.signingKey = jwtSecret.getBytes(StandardCharsets.UTF_8);
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public String issueToken(Long userId, String username, List<String> roles) {
        Instant now = clock.instant();
        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(userId));
        claims.put("username", username);
        claims.put("roles", roles == null ? List.of() : roles);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(tokenTtlSeconds).getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedClaims = encodeJson(claims);
        String signingInput = encodedHeader + "." + encodedClaims;
        return signingInput + "." + sign(signingInput);
    }

    public Optional<AuthenticatedUser> parseToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] segments = token.split("\\.");
        if (segments.length != 3) {
            return Optional.empty();
        }

        String signingInput = segments[0] + "." + segments[1];
        if (!signatureMatches(signingInput, segments[2])) {
            return Optional.empty();
        }

        try {
            Map<String, Object> claims = objectMapper.readValue(
                    BASE64_URL_DECODER.decode(segments[1]),
                    CLAIMS_TYPE
            );
            long expiresAt = numericClaim(claims.get("exp"));
            if (expiresAt <= clock.instant().getEpochSecond()) {
                return Optional.empty();
            }
            Long userId = Long.valueOf(String.valueOf(claims.get("sub")));
            String username = String.valueOf(claims.getOrDefault("username", ""));
            List<String> roles = rolesClaim(claims.get("roles"));
            return Optional.of(new AuthenticatedUser(userId, username, roles));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String encodeJson(Object value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to encode JWT payload", ex);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey, HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }

    private boolean signatureMatches(String signingInput, String receivedSignature) {
        byte[] expected = sign(signingInput).getBytes(StandardCharsets.US_ASCII);
        byte[] received = receivedSignature.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, received);
    }

    private long numericClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private List<String> rolesClaim(Object value) {
        if (value instanceof List<?> values) {
            return values.stream()
                    .map(String::valueOf)
                    .filter(role -> !role.isBlank())
                    .toList();
        }
        return List.of();
    }
}
