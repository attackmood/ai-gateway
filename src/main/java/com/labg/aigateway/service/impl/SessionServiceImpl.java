package com.labg.aigateway.service.impl;

import com.labg.aigateway.domain.ChatSession;
import com.labg.aigateway.domain.Message;
import com.labg.aigateway.repository.ChatSessionRepository;
import com.labg.aigateway.repository.QueryHistoryRepository;
import com.labg.aigateway.service.AiEngineClient;
import com.labg.aigateway.service.CacheService;
import com.labg.aigateway.service.ContextManager;
import com.labg.aigateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : SessionService
 * author         : 이가은
 * date           : 2025-10-28
 * description    : 채팅 세션 생명주기 관리(세션 CRUD)
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionServiceImpl implements SessionService {

    private final AiEngineClient aiEngineClient;
    private final ChatSessionRepository sessionRepository;
    private final QueryHistoryRepository historyRepository;
    private final CacheService cacheService;
    private final ContextManager contextManager;


    /**
     * 새로운 세션 생성
     *
     * @param userId 사용자 ID
     * @return 생성된 세션
     */
    @Override
    public Mono<ChatSession> createSession(String userId) {
        String sessionId = generateSessionId();

        ChatSession session = ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .maxContextWindow(10)
                .build();

        log.info("새 세션 생성 - sessionId: {}, userId: {}", sessionId, userId);

        return sessionRepository.save(session)
                .doOnSuccess(saved -> cacheService.cacheSession(saved).subscribe())
                .doOnError(error -> log.error("세션 생성 실패 - sessionId: {}", sessionId, error));
    }


    /**
     * 세션 조회 또는 생성
     * 1. 캐시 확인
     * 2. DB 조회
     * 3. 없으면 새로 생성
     *
     * @param sessionId 세션 ID (null 가능)
     * @param userId    사용자 ID
     * @return 세션
     */
    @Override
    public Mono<ChatSession> getOrCreateSession(String sessionId, String userId) {
        // sessionId가 없으면 새로 생성
        if (sessionId == null || sessionId.isBlank()) {
            log.info("sessionId 없음 - 새 세션 생성");
            return createSession(userId);
        }

        // 1. 캐시 확인
        return cacheService.getCachedSession(sessionId)
                .doOnNext(cached -> log.debug("캐시된 세션 조회 성공 - sessionId: {}", sessionId))
                // 2. 캐시 미스 시 DB 조회
                .switchIfEmpty(
                        sessionRepository.findBySessionId(sessionId)
                                .doOnNext(session -> {
                                    log.debug("DB에서 세션 조회 성공 - sessionId: {}", sessionId);
                                    // 캐시에 저장
                                    cacheService.cacheSession(session).subscribe();
                                })
                                // 3. DB에도 없으면 새로 생성
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            log.warn("세션을 찾을 수 없음 - 새 세션 생성. sessionId: {}", sessionId);
                                            return createSession(userId);
                                        })
                                )
                )
                // lastAccessedAt 업데이트
                .flatMap(this::updateLastAccessed);
    }

    /**
     * @param sessionId
     * @param message
     * @return
     */
    @Override
    public Mono<ChatSession> addMessage(String sessionId, Message message) {
        return sessionRepository.findBySessionId(sessionId)
                .switchIfEmpty(Mono.error(
                        new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId)
                ))
                .flatMap(session -> {
                    // 메시지 추가 (Domain의 비즈니스 로직 실행)
                    session.addMessage(message);

                    // 컨텍스트 정리 필요 여부 확인
                    if (contextManager.shouldTruncateContext(session)) {
                        log.debug("컨텍스트 윈도우 초과 - 정리 실행. sessionId: {}", sessionId);
                    }

                    log.debug("메시지 추가 - sessionId: {}, role: {}, content length: {}",
                            sessionId, message.getRole(), message.getContent().length());

                    // MongoDB 저장
                    return sessionRepository.save(session);
                })
                .doOnSuccess(session -> {
                    // 캐시 업데이트
                    cacheService.cacheSession(session).subscribe();
                    // 쿼리 캐시 무효화 (새 메시지가 추가되었으므로)
//                    cacheService.invalidateQueryCache(sessionId).subscribe();
                })
                .doOnError(error ->
                        log.error("메시지 추가 실패 - sessionId: {}", sessionId, error)
                );
    }


    /**
     * 사용자 메시지와 AI 응답을 한 번에 추가
     * @param sessionId
     * @param userMessage
     * @param assistantMessage
     * @return
     */
    @Override
    public Mono<ChatSession> addMessagePair(String sessionId, Message userMessage, Message assistantMessage) {

        return addMessage(sessionId, userMessage)
                .flatMap(session -> addMessage(sessionId, assistantMessage));
    }

    /**
     * 세션 조회 (캐시 우선)
     *
     * @param sessionId 세션 ID
     * @return 세션 (없으면 empty)
     */
    public Mono<ChatSession> getSession(String sessionId) {
        return cacheService.getCachedSession(sessionId)
                .switchIfEmpty(
                        sessionRepository.findBySessionId(sessionId)
                                .doOnNext(session -> cacheService.cacheSession(session).subscribe())
                );
    }


    /**
     * 만료된 세션 삭제 (24시간 이상 미사용)
     * @return
     */
    @Scheduled(cron = "${session.cleanup-cron:0 0 3 * * ?}")
    public void cleanExpiredSessions() {
        log.info("만료된 세션 정리 시작");

        LocalDateTime expiryTime = LocalDateTime.now().minusHours(24);

        sessionRepository.findByLastAccessedAtBefore(expiryTime)
                .flatMap(session -> {
                    log.debug("만료된 세션 삭제 - sessionId: {}, lastAccessed: {}",
                              session.getSessionId(), session.getLastAccessedAt());
                    return sessionRepository.delete(session)
                            .thenReturn(session.getSessionId());
                })
                .collectList()
                .doOnSuccess(deletedIds -> {
                    log.info("만료된 세션 정리 완료 - 삭제된 세션 수: {}", deletedIds.size());
                    // 캐시에서도 제거
                    deletedIds.forEach(sessionId ->
                        cacheService.invalidateCache(sessionId).subscribe()
                    );
                })
                .doOnError(error -> log.error("만료된 세션 정리 실패", error))
                .subscribe();
    }

    /**
     * 세션 존재 여부 확인
     * @param sessionId
     * @return
     */
    @Override
    public Mono<Boolean> sessionExists(String sessionId) {
        return null;
    }


    /**
     * lastAccessedAt 업데이트
     *
     * @param session 세션
     * @return 업데이트된 세션
     */
    private Mono<ChatSession> updateLastAccessed(ChatSession session) {
        session.setLastAccessedAt(LocalDateTime.now());
        return sessionRepository.save(session)
                .doOnSuccess(updated -> cacheService.cacheSession(updated).subscribe());
    }

    /**
     * 세션 ID 생성
     *
     * @return UUID 기반 세션 ID
     */
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().replace("-", "");
    }
}
