package com.telegram.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validates Telegram Mini App initData using HMAC-SHA256
 */
@Slf4j
@Service
public class TelegramInitDataValidator {

    @Value("${telegram.bot.token:}")
    private String botToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<Long> validateAndExtractUserId(String initData) {
        if (initData == null || initData.isBlank()) {
            return Optional.empty();
        }

        try {
            Map<String, String> params = parseInitData(initData);
            String hash = params.get("hash");
            if (hash == null) return Optional.empty();

            String dataCheckString = params.entrySet().stream()
                    .filter(e -> !"hash".equals(e.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("\n"));

            byte[] secretKey = hmacSha256("WebAppData".getBytes(StandardCharsets.UTF_8), botToken.getBytes(StandardCharsets.UTF_8));
            String computedHash = bytesToHex(hmacSha256(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

            if (!computedHash.equals(hash)) {
                log.warn("Invalid Telegram initData hash");
                return Optional.empty();
            }

            long authDate = Long.parseLong(params.getOrDefault("auth_date", "0"));
            long now = System.currentTimeMillis() / 1000;
            if (now - authDate > 86400) {
                log.warn("Telegram initData expired");
                return Optional.empty();
            }

            String userJson = params.get("user");
            if (userJson == null) return Optional.empty();

            JsonNode userNode = objectMapper.readTree(userJson);
            return Optional.of(userNode.get("id").asLong());
        } catch (Exception e) {
            log.error("Failed to validate initData: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, String> parseInitData(String initData) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = initData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }
        return params;
    }

    private byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
