package com.labg.aigateway.service.impl;

import com.labg.aigateway.domain.ChatSession;
import com.labg.aigateway.dto.response.AiResponse;
import com.labg.aigateway.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import reactor.core.publisher.Flux;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : CacheService
 * author         : 이가은
 * date           : 2025-10-28
 * description    : Redis 기반 캐싱 관리(ReactiveRedisTemplate)
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheServiceImpl implements CacheService {
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    @Value("${cache.ttl:5m}")
    private Duration cacheTtl;

    @Value("${session.cache-ttl:10m}")
    private Duration sessionTtl;

    /**
     * 캐시된 응답 조회
     * @param sessionId
     * @param message
     * @return
     */
    @Override
    public Mono<AiResponse> getCachedResponse(String sessionId, String message) {
        final String key = generateCacheKey(sessionId, message);
        return redisTemplate.opsForValue().get(key)
                .cast(AiResponse.class)
                .doOnNext(v -> log.debug("Cache HIT: {}", key))
                .doOnError(e -> log.warn("Cache GET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 응답 캐싱(TTL 5분)
     * @param sessionId
     * @param message
     * @param response
     * @return
     */
    @Override
    public Mono<Boolean> cacheResponse(String sessionId, String message, AiResponse response) {
        final String key = generateCacheKey(sessionId, message);
        return redisTemplate.opsForValue().set(key, response, cacheTtl)
                .doOnError(e -> log.warn("Cache SET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * 캐시된 세션 조회
     * @param sessionId
     * @return
     */
    @Override
    public Mono<ChatSession> getCachedSession(String sessionId) {
        final String key = sessionKey(sessionId);
        return redisTemplate.opsForValue().get(key)
                .cast(ChatSession.class)
                .doOnNext(v -> log.debug("Session Cache HIT: {}", key))
                .doOnError(e -> log.warn("Session Cache GET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 세션 캐싱 (TTL 10분)
     * @param session
     * @return
     */
    @Override
    public Mono<Boolean> cacheSession(ChatSession session) {
        final String key = sessionKey(session.getSessionId());
        return redisTemplate.opsForValue().set(key, session, sessionTtl)
                .doOnError(e -> log.warn("Session Cache SET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * 특정 세션의 캐시 무효화
     * @param sessionId
     * @return
     */
    @Override
    public Mono<Boolean> invalidateCache(String sessionId) {
        final String pattern = "query:" + sessionId + ":*";
        final String sessionKey = sessionKey(sessionId);

        Flux<String> keysToDelete = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).build())
                .concatWith(Flux.just(sessionKey));

        return redisTemplate.delete(keysToDelete)
                .map(deletedCount -> deletedCount != null && deletedCount > 0)
                .doOnError(e -> log.warn("Cache invalidate failed (skip): sessionId={} - {}", sessionId, e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * 캐시 키 생성
     * @param sessionId
     * @param message
     * @return
     */
    @Override
    public String generateCacheKey(String sessionId, String message) {
        String normalized = Objects.toString(message, "").trim();
        String msgHash = sha256Hex(normalized);
        return "query:" + sessionId + ":" + msgHash;
    }

    private String sessionKey(String sessionId) {
        return "session:" + sessionId;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // 불가능하지만, 예외 시 안전하게 fallback
            return Integer.toHexString(input.hashCode());
        }
    }
}
