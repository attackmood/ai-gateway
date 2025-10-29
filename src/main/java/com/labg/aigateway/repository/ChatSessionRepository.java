package com.labg.aigateway.repository;

import com.labg.aigateway.domain.ChatSession;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * packageName    : com.labg.aigateway.repository
 * fileName       : ChatSessionRepository
 * author         : 이가은
 * date           : 2025-10-28
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
public interface ChatSessionRepository extends ReactiveMongoRepository<ChatSession, String> {

    Mono<ChatSession> findBySessionId(String sessionId);

    Flux<ChatSession> findByLastAccessedAtBefore(LocalDateTime expiryTime);
}
