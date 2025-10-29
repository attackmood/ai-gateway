package com.labg.aigateway.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 * packageName    : com.labg.aigateway.domain
 * fileName       : QueryHistory
 * author         : 이가은
 * date           : 2025-10-28
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "query_history")
public class QueryHistory {
    private ChatSession session;
    private String userQuery;
    private String aiResponse;
    private Double processingTime;
    private List<String> toolsUsed;
    private LocalDateTime timestamp;


}
