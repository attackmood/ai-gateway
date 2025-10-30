package com.labg.aigateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * packageName    : com.labg.aigateway.dto.response
 * fileName       : AiResponse
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiResponse {
    private boolean success;

    private String message;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("processing_time")
    private Double processingTime;

    @JsonProperty("mode_used")
    private String modeUsed;

    private Metadata metadata;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {

        @JsonProperty("complexity_score")
        private Double complexityScore;

        @JsonProperty("selected_tools")
        private List<String> selectedTools;

        @JsonProperty("tool_results")
        private List<ToolResult> toolResults;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolResult {
        private String tool;
        private String result;
        private Double score;

        @JsonProperty("execution_time")
        private Double executionTime;
    }

    // 에러 응답 체크
    public boolean isError() {
        return !success;
    }
}
