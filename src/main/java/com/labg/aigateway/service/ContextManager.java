package com.labg.aigateway.service;

import com.labg.aigateway.domain.ChatSession;
import com.labg.aigateway.domain.Message;
import com.labg.aigateway.dto.request.AiEngineRequest;

import java.util.List;
import java.util.Map;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : ContextManager
 * author         : 이가은
 * date           : 2025-10-29
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-29          이가은             최초 생성
 */
public interface ContextManager {
    List<Message> getRecentContext(ChatSession session, int maxMessages);
    Map<String, Object> formatContextForAi(List<Message> messages);
    List<Message> truncateByTokenLimit(List<Message> messages, int maxTokens);
    boolean shouldTruncateContext(ChatSession session);

}
