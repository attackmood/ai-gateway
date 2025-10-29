package com.labg.aigateway.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * packageName    : com.labg.aigateway.domain
 * fileName       : Message
 * author         : 이가은
 * date           : 2025-10-28
 * description    : ChatSession 내부에 포함되는 메시지(MongoDB에 embedded document로 저장)
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private String role;  // "user" or "assistant"

    private String content;

    private LocalDateTime timestamp;

    private MessageMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageMetadata {
        private Double processingTime;
        private String modeUsed;
        private Double complexityScore;
    }

    // === 팩토리 메서드 ===

    public static Message userMessage(String content) {
        return Message.builder()
                .role("user")
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static Message assistantMessage(String content, MessageMetadata metadata) {
        return Message.builder()
                .role("assistant")
                .content(content)
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }
}
