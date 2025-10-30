package com.labg.aigateway.service.impl;

import com.labg.aigateway.domain.ChatSession;
import com.labg.aigateway.domain.Message;
import com.labg.aigateway.service.ContextManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.*;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : ContextManager
 * author         : 이가은
 * date           : 2025-10-28
 * description    : 대화 컨텍스트 관리 및 최적화
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContextManagerImpl implements ContextManager {

    /**
     * 최근 N개 메시지 추출 (기본 10개)
     * @param session
     * @param maxMessages
     * @return
     */
    @Override
    public List<Message> getRecentContext(ChatSession session, int maxMessages) {
        if (session == null || session.getMessages() == null || session.getMessages().isEmpty()) {
            return List.of();
        }
        int count = Math.max(0, maxMessages);
        List<Message> recent = session.getRecentMessages(count);
        return recent == null ? List.of() : recent;
    }

    /**
     * Domain Message → DTO ContextMessage 변환: AI Engine에 전달할 컨텍스트 포맷팅
     * @param messages
     * @return
     */
    @Override
    public Map<String, Object> formatContextForAi(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;  // null이면 JSON에서 제외됨
        }

        List<Map<String, Object>> messageList = new ArrayList<>();

        for (Message msg : messages) {
            if (msg == null) continue;

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("role", Objects.toString(msg.getRole(), ""));
            messageMap.put("content", Objects.toString(msg.getContent(), ""));

            if (msg.getTimestamp() != null) {
                long epochSec = msg.getTimestamp()
                    .atZone(ZoneId.systemDefault())
                    .toEpochSecond();
                messageMap.put("timestamp", epochSec);
            } else {
                messageMap.put("timestamp", 0L);
            }

            messageList.add(messageMap);
        }

        Map<String, Object> context = new HashMap<>();
        context.put("messages", messageList);

        return context;
    }


    /**
     * 토큰 제한에 맞게 메시지 자르기
     * @param messages
     * @param maxTokens
     * @return
     */
    @Override
    public List<Message> truncateByTokenLimit(List<Message> messages, int maxTokens) {
        if (messages == null || messages.isEmpty()) return List.of();
        int budget = Math.max(0, maxTokens);
        List<Message> result = new ArrayList<>();
        // 최신 메시지 우선 보존: 뒤에서 앞으로 누적, 예산 내에서 추가
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (m == null) continue;
            int cost = estimateTokensForText(Objects.toString(m.getContent(), ""));
            if (cost <= budget) {
                result.add(m);
                budget -= cost;
            } else {
                // 남은 예산이 일부라도 있으면 잘라서 포함할 수도 있으나, 간단화를 위해 생략
                break;
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * 컨텍스트 정리 필요 여부 판단
     * @param session
     * @return
     */
    @Override
    public boolean shouldTruncateContext(ChatSession session) {
        if (session == null || session.getMessages() == null) return false;
        List<Message> all = session.getMessages();
        // 정책 1: 토큰 수 4000 초과 시 정리
        if (estimateTokenCount(all) > 4000) return true;
        // 정책 2: 최대 컨텍스트 윈도우 초과 시 정리 (user/assistant 쌍 고려해 2배까지 허용)
        Integer window = session.getMaxContextWindow();
        return window != null && all.size() > window * 2;
    }


    // ===== 내부 유틸 =====

    /**
     * 토큰 수 추정 (대략적, 한글 1글자 -> 2토큰)
     * @param messages
     * @return
     */
    private int estimateTokenCount(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return 0;
        long total = 0;
        for (Message m : messages) {
            if (m == null) continue;
            String text = m.getContent();
            if (text == null || text.isEmpty()) continue;
            total += estimateTokensForText(text);
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private int estimateTokensForText(String text) {
        if (text == null || text.isEmpty()) return 0;
        int tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isCjkOrKorean(ch)) {
                tokens += 2; // 한글/중국어/일본어: 대략 2토큰
            } else {
                tokens += 1; // 기타 문자: 대략 1토큰
            }
        }
        // 약간의 프롬프트/메타 오버헤드 보정
        return tokens + 4;
    }

    private boolean isCjkOrKorean(char ch) {
        // 한글 자모, 한글 음절, CJK 통합 한자, 히라가나/가타카나 범위
        return (ch >= '\u1100' && ch <= '\u11FF')   // Hangul Jamo
                || (ch >= '\u3130' && ch <= '\u318F') // Hangul Compatibility Jamo
                || (ch >= '\uAC00' && ch <= '\uD7AF') // Hangul Syllables
                || (ch >= '\u3040' && ch <= '\u309F') // Hiragana
                || (ch >= '\u30A0' && ch <= '\u30FF') // Katakana
                || (ch >= '\u4E00' && ch <= '\u9FFF'); // CJK Unified Ideographs
    }
}
