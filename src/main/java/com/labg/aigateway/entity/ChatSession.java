package com.labg.aigateway.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * packageName    : com.labg.aigateway.domain
 * fileName       : ChatSession
 * author         : 이가은
 * date           : 2025-10-28
 * description    : MongoDB에 저장되는 채팅 세션
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSession {

    @Id
    private String id;  // MongoDB _id

    @Indexed(unique = true)
    private String sessionId;  // 비즈니스 세션 ID

    @Indexed
    private String userId;

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    private LocalDateTime createdAt;

    private LocalDateTime lastAccessedAt;

    @Builder.Default
    private Integer maxContextWindow = 10;  // 최근 N개 메시지만 유지

    // === 비즈니스 로직 ===



    /**
     * 새 세션 추가
     * @param userId
     */
    public static ChatSession newSession(String userId) {
        String sessionId = "session_" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();

        return ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .createdAt(now)
                .lastAccessedAt(now)
                .maxContextWindow(10) // 기본값 설정
                .build();
    }

    /**
     * 새 메시지 추가
     */
    public void addMessage(Message message) {
        this.messages.add(message);
        this.lastAccessedAt = LocalDateTime.now();

        // 컨텍스트 윈도우 초과 시 오래된 메시지 제거
        if (messages.size() > maxContextWindow * 2) {
            this.messages = new ArrayList<>(
                messages.subList(
                    messages.size() - (maxContextWindow * 2),
                    messages.size()
                )
            );
        }
    }

    /**
     * 최근 N개 메시지만 가져오기 (컨텍스트용)
     */
    public List<Message> getRecentMessages(int count) {
        int size = messages.size();
        int fromIndex = Math.max(0, size - count);
        return new ArrayList<>(messages.subList(fromIndex, size));
    }

    /**
     * 세션이 만료되었는지 확인 (24시간)
     */
    @JsonIgnore
    public boolean isExpired() {
        return lastAccessedAt.isBefore(LocalDateTime.now().minusHours(24));
    }
}
