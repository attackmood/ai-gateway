package com.labg.aigateway.handler;

import com.labg.aigateway.domain.Message;
import com.labg.aigateway.dto.request.AiEngineRequest;
import com.labg.aigateway.dto.request.ChatRequest;
import com.labg.aigateway.dto.response.ChatResponse;
import com.labg.aigateway.service.AiEngineClient;

import java.util.List;
import java.util.Map;

import com.labg.aigateway.service.ContextManager;
import com.labg.aigateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

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
public class ChatHandler {

    private final AiEngineClient aiEngineClient;
    private final SessionService sessionService;
    private final ContextManager contextManager;


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
                    return sessionService.getOrCreateSession(chatRequest.getSessionId(),chatRequest.getUserId())
                            .flatMap(session -> {
                                // 2. 사용자 메시지 생성
                                Message userMessage = Message.userMessage(chatRequest.getMessage());

                                // 3. 컨텍스트 추출
                                int window = session.getMaxContextWindow() == null ? 10 : session.getMaxContextWindow();
                                List<Message> context = contextManager.getRecentContext(session, window);
                                // 3-1. 토큰 제한 적용(최대 4000 토큰)
                                List<Message> limited = contextManager.truncateByTokenLimit(context, 4000);

                                // 4. AI Engine 호출
                                AiEngineRequest aiRequest = AiEngineRequest.builder()
                                        .message(chatRequest.getMessage())
                                        .sessionId(session.getSessionId())
                                        .context(contextManager.formatContextForAi(limited))
                                        .build();

                                return aiEngineClient.query(aiRequest)
                                        .flatMap(aiResponse -> {
                                            // 5. AI 응답을 메시지로 변환
                                            Message assistantMessage = Message.assistantMessage(
                                                    aiResponse.getMessage(),
                                                    Message.MessageMetadata.builder()
                                                            .processingTime(aiResponse.getProcessingTime())
                                                            .modeUsed(aiResponse.getModeUsed())
                                                            .build()
                                            );

                                            // 6. 메시지 쌍 저장
                                            return sessionService.addMessagePair(
                                                    session.getSessionId(),
                                                    userMessage,
                                                    assistantMessage
                                            ).thenReturn(aiResponse);
                                        });
                            })
                            .flatMap(aiResponse -> {
                                // 7. 최종 응답 생성
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
