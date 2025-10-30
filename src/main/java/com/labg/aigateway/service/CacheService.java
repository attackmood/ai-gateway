package com.labg.aigateway.service;

import com.labg.aigateway.domain.ChatSession;
import com.labg.aigateway.dto.response.AiResponse;
import reactor.core.publisher.Mono;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : CacheService
 * author         : 이가은
 * date           : 2025-10-29
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-29          이가은             최초 생성
 */
public interface CacheService {
    Mono<AiResponse> getCachedResponse(String sessionId, String message);
    Mono<Boolean> cacheResponse(String sessionId, String message, AiResponse response);
    Mono<ChatSession> getCachedSession(String sessionId);
    Mono<Boolean> cacheSession(ChatSession session);
    Mono<Boolean> invalidateCache(String sessionId);
    Mono<Boolean> invalidateQueryCache(String sessionId);
}
