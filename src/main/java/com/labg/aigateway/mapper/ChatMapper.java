package com.labg.aigateway.mapper;

import com.labg.aigateway.domain.ChatSession;
import com.labg.aigateway.domain.Message;
import com.labg.aigateway.dto.request.AiEngineRequest;
import com.labg.aigateway.dto.response.AiResponse;
import com.labg.aigateway.dto.response.ChatResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * packageName    : com.labg.aigateway.mapper
 * fileName       : ChatMapper
 * author         : 이가은
 * date           : 2025-10-29
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-29          이가은             최초 생성
 */
public class ChatMapper {

    /**
     * Domain → DTO (AI Engine 요청용)
     */
    public static AiEngineRequest toAiEngineRequest(
            String message,
            ChatSession session) {

        Map<String, Object> context = null;

        // 대화 히스토리가 있으면 context에 추가
        if (session.getMessages() != null && !session.getMessages().isEmpty()) {
            List<Map<String, Object>> messages = new ArrayList<>();

            for (Message msg : session.getMessages()) {
                Map<String, Object> messageMap = new HashMap<>();
                messageMap.put("role", msg.getRole());
                messageMap.put("content", msg.getContent());
                messageMap.put("timestamp", msg.getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC));
                messages.add(messageMap);
            }

            context = new HashMap<>();
            context.put("messages", messages);
        }

        return AiEngineRequest.builder()
                .message(message)
                .sessionId(session.getSessionId())
                .mode("parallel")
                .context(context)
                .build();
    }

    /**
     * DTO → Domain (메시지 저장용)
     */
    public static Message aiResponseToMessage(AiResponse aiResponse) {
        Message.MessageMetadata metadata = Message.MessageMetadata.builder()
                .processingTime(aiResponse.getProcessingTime())
                .modeUsed(aiResponse.getModeUsed())
                .complexityScore(aiResponse.getMetadata() != null ?
                        aiResponse.getMetadata().getComplexityScore() : null)
                .build();

        return Message.assistantMessage(aiResponse.getMessage(), metadata);
    }

    /**
     * DTO → DTO (최종 응답 생성)
     */
    public static ChatResponse toChatResponse(AiResponse aiResponse) {
        Map<String, Object> metadata = new HashMap<>();

        if (aiResponse.getMetadata() != null) {
            metadata.put("complexity_score", aiResponse.getMetadata().getComplexityScore());
            metadata.put("selected_tools", aiResponse.getMetadata().getSelectedTools());
        }

        return ChatResponse.success(
                aiResponse.getMessage(),
                aiResponse.getSessionId(),
                aiResponse.getProcessingTime(),
                metadata
        );
    }
}