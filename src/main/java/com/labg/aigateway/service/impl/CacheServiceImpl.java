package com.labg.aigateway.service.impl;

import com.labg.aigateway.entity.ChatSession;
import com.labg.aigateway.dto.response.AiResponse;
import com.labg.aigateway.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;

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
    private static final Duration QUERY_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration SESSION_CACHE_TTL = Duration.ofMinutes(10);

    /**
     * 캐시된 응답 조회
     *
     * @param sessionId
     * @param message
     * @return
     */
    @Override
    public Mono<AiResponse> getCachedResponse(String sessionId, String message) {
        final String key = generateCacheKey(sessionId, message);
        return redisTemplate.opsForValue().get(key)
                .cast(AiResponse.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Query Cache MISS: {}", key);
                    return Mono.empty();
                }))
                .doOnNext(v -> log.debug("Query Cache HIT: {}", key))
                .doOnError(e -> log.warn("Query Cache GET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 응답 캐싱(TTL 5분)
     *
     * @param sessionId
     * @param message
     * @param response
     * @return
     */
    @Override
    public Mono<Boolean> cacheResponse(String sessionId, String message, AiResponse response) {
        final String key = generateCacheKey(sessionId, message);
        return redisTemplate.opsForValue().set(key, response, QUERY_CACHE_TTL)
                .doOnSuccess(success -> log.debug("Query Cache SET- key: {}, ttl: {}분", key, QUERY_CACHE_TTL.toMinutes()))
                .doOnError(e -> log.warn("Query Cache SET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * 캐시된 세션 조회
     *
     * @param sessionId
     * @return
     */
    @Override
    public Mono<ChatSession> getCachedSession(String sessionId) {
        final String key = sessionKey(sessionId);
        return redisTemplate.opsForValue().get(key)
                .cast(ChatSession.class)
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Session Cache MISS: {}", key);
                    return Mono.empty();
                }))
                .doOnNext(v -> log.debug("Session Cache HIT: {}", key))
                .doOnError(e -> log.warn("Session Cache GET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    /**
     * 세션 캐싱 (TTL 10분)
     * Description: DB에 저장된 최신 세션을 Redis 캐시에 갱신해 이후 조회를 캐시에서 받을 수 있게 함
     *
     * @param session
     * @return
     */
    @Override
    public Mono<Boolean> cacheSession(ChatSession session) {
        final String key = sessionKey(session.getSessionId());
        return redisTemplate.opsForValue().set(key, session, SESSION_CACHE_TTL) // 레디스에 저장
                .doOnSuccess(ok -> log.debug("Session Cache SET: {} -> {}", key, ok))
                .doOnError(e -> log.warn("Session Cache SET failed (skip): {} - {}", key, e.getMessage()))
                .onErrorReturn(false);
    }


    /**
     * 세션 관련 모든 캐시 무효화
     */
    @Override
    public Mono<Boolean> invalidateCache(String sessionId) {
        String sessionKey = sessionKey(sessionId);

        return redisTemplate.delete(sessionKey)
                .map(count -> count > 0)
                .doOnSuccess(deleted -> log.debug("캐시 무효화 - sessionId: {}, deleted: {}", sessionId, deleted));
    }

    /**
     * 쿼리 캐시만 무효화 (세션에 새 메시지 추가 시)
     */
    @Override
    public Mono<Boolean> invalidateQueryCache(String sessionId) {
        String pattern = "query:" + sessionId + ":*";

        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .reduce(0L, Long::sum)
                .map(count -> count > 0)
                .doOnSuccess(deleted ->
                        log.debug("쿼리 캐시 무효화 - sessionId: {}, count: {}", sessionId, deleted)
                );
    }


    /**
     * 특정 세션의 캐시 무효화
     * 컨텍스트가 바뀐 뒤에도 이전 질의 응답 캐시가 재사용되는 오류(오답 재사용)
     *
     * @param sessionId
     * @return
     */
    public Mono<Boolean> invalidateCacheNoUse(String sessionId) {
        final String pattern = "query:" + sessionId + ":*";
        Flux<String> keysToDelete = redisTemplate.scan(ScanOptions.scanOptions().match(pattern).build());

        return redisTemplate.delete(keysToDelete)
                .map(deletedCount -> deletedCount != null && deletedCount > 0)
                .doOnError(e -> log.warn("Cache invalidate failed (skip): sessionId={} - {}", sessionId, e.getMessage()))
                .onErrorReturn(false);
    }

    /**
     * 캐시 키 생성
     *
     * @param sessionId
     * @param message
     * @return
     */
    private String generateCacheKey(String sessionId, String message) {
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
