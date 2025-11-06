package com.labg.aigateway.service;

import com.labg.aigateway.entity.ChatSession;
import com.labg.aigateway.entity.Message;
import reactor.core.publisher.Mono;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : SessionService
 * author         : 이가은
 * date           : 2025-10-29
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-29          이가은             최초 생성
 */
public interface SessionService {
    Mono<ChatSession> getOrCreateSession(String sessionId, String userId);
    Mono<ChatSession> addMessage(String sessionId, Message message);
    Mono<ChatSession> addMessagePair(String sessionId, Message userMessage, Message assistantMessage);

}
