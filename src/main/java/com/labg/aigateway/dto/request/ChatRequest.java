package com.labg.aigateway.dto.request;

import lombok.*;

/**
 * packageName    : com.labg.aigateway.dto.request
 * fileName       : ChatRequest
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
public class ChatRequest {
    private String message;

    private String sessionId;  // null이면 새 세션 생성

    private String userId;     // 인증 후 채워짐
}
