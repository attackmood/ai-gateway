package com.labg.aigateway.handler;

import com.labg.aigateway.domain.Message;
import com.labg.aigateway.dto.request.AiEngineRequest;
import com.labg.aigateway.dto.request.ChatRequest;
import com.labg.aigateway.dto.response.ChatResponse;
import com.labg.aigateway.service.AiEngineClient;
import com.labg.aigateway.service.CacheService;
import com.labg.aigateway.service.ContextManager;
import com.labg.aigateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * packageName    : com.labg.aigateway.handler
 * fileName       : ChatHandler
 * author         : 이가은
 * date           : 2025-10-28
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatHandler {

    private final AiEngineClient aiEngineClient;
    private final SessionService sessionService;
    private final ContextManager contextManager;
    private final CacheService cacheService;


    public Mono<ServerResponse> handleChat(ServerRequest request) {
        return request.bodyToMono(ChatRequest.class)
                .flatMap(chatRequest -> {
                    // 입력 검증: message 필수
                    if (!StringUtils.hasText(chatRequest.getMessage())) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "error", "Invalid request",
                                        "detail", "Field 'message' is required"
                                ));
                    }
                    // 1. 세션 조회/생성
                    return sessionService.getOrCreateSession(chatRequest.getSessionId(), chatRequest.getUserId())
                            .flatMap(session -> {
                                // 2. 사용자 메시지 생성
                                Message userMessage = Message.userMessage(chatRequest.getMessage());

                                // 3. 캐시 조회
                                return cacheService.getCachedResponse(session.getSessionId(), chatRequest.getMessage())
                                        .flatMap(cachedResponse -> {
                                            // 캐시 HIT: 메시지 저장 없이 바로 응답 반환
                                            log.debug("캐시 HIT - 메시지 저장 생략, 즉시 응답 반환");
                                            return Mono.just(cachedResponse);
                                        })
                                        .switchIfEmpty(
                                                // 캐시 MISS 시: 컨텍스트 추출 → AI 호출 → 캐시 저장 → 메시지 저장
                                                Mono.defer(() -> {
                                                    // 3-1. 컨텍스트 추출 (캐시 MISS일 때만 실행)
                                                    int window = session.getMaxContextWindow() == null ? 10 : session.getMaxContextWindow();
                                                    List<Message> context = contextManager.getRecentContext(session, window);
                                                    // 3-2. 토큰 제한 적용(최대 4000 토큰)
                                                    List<Message> limited = contextManager.truncateByTokenLimit(context, 4000);

                                                    // 3-3. AI 요청 생성
                                                    AiEngineRequest aiRequest = AiEngineRequest.builder()
                                                            .message(chatRequest.getMessage())
                                                            .sessionId(session.getSessionId())
                                                            .context(contextManager.formatContextForAi(limited))
                                                            .build();

                                                    // 3-4. AI Engine 호출 및 캐시 저장
                                                    return aiEngineClient.query(aiRequest)
                                                            .flatMap(aiResponse ->
                                                                    cacheService.cacheResponse(session.getSessionId(), chatRequest.getMessage(), aiResponse)
                                                                            .onErrorReturn(false)
                                                                            .thenReturn(aiResponse)
                                                            )
                                                            .flatMap(aiResponse -> {
                                                                // 3-5. AI 응답을 메시지로 변환
                                                                Message assistantMessage = Message.assistantMessage(
                                                                        aiResponse.getMessage(),
                                                                        Message.MessageMetadata.builder()
                                                                                .processingTime(aiResponse.getProcessingTime())
                                                                                .modeUsed(aiResponse.getModeUsed())
                                                                                .build()
                                                                );

                                                                // 3-6. 메시지 쌍 저장 (addMessagePair에서 쿼리 캐시 무효화도 처리)
                                                                return sessionService.addMessagePair(
                                                                        session.getSessionId(),
                                                                        userMessage,
                                                                        assistantMessage
                                                                ).thenReturn(aiResponse);
                                                            });
                                                })
                                        );
                            })
                            .flatMap(aiResponse -> {
                                // 6. 최종 응답 생성
                                ChatResponse response = ChatResponse.success(
                                        aiResponse.getMessage(),
                                        aiResponse.getSessionId(),
                                        aiResponse.getProcessingTime(),
                                        Map.of()
                                );
                                return ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(response);
                            });
                });
    }


}
