package com.labg.aigateway.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * packageName    : com.labg.aigateway.dto.response
 * fileName       : ChatResponse
 * author         : 이가은
 * date           : 2025-10-28
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private boolean success;

    private String message;          // AI의 답변

    private String sessionId;

    private LocalDateTime timestamp;

    private Double processingTime;   // 처리 시간(초)

    private String modeUsed;         // "parallel", "simple", "llm_integrated"

    private Map<String, Object> metadata;  // 추가 정보

    // 에러 응답용 생성자
    public static ChatResponse error(String message) {
        return ChatResponse.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    // 성공 응답용 생성자
    public static ChatResponse success(String message, String sessionId,
                                       Double processingTime, Map<String, Object> metadata) {
        return ChatResponse.builder()
                .success(true)
                .message(message)
                .sessionId(sessionId)
                .timestamp(LocalDateTime.now())
                .processingTime(processingTime)
                .metadata(metadata)
                .build();
    }
}
