package com.labg.aigateway.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * packageName    : com.labg.aigateway.dto.request
 * fileName       : AiEngineRequest
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
@JsonInclude(JsonInclude.Include.NON_NULL)  // ✅ null 필드는 JSON에서 제외
public class AiEngineRequest {
    private String message;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("mode")
    @Builder.Default
    private String mode = "parallel";  // ✅ 기본값 설정

    @JsonProperty("context")
    private Map<String, Object> context;  // ✅ List가 아닌 Map

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContextMessage {
        private String role;      // "user" or "assistant"
        private String content;
        private Long timestamp;
    }
}
