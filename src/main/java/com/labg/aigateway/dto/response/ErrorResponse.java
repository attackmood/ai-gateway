package com.labg.aigateway.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * packageName    : com.labg.aigateway.dto.response
 * fileName       : ErrorResponse
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Builder.Default
    private boolean success = false;

    private String error;           // HTTP 상태 문구 (Bad Request, Internal Server Error)

    private String message;         // 사용자에게 보여줄 메시지

    private String path;            // 요청 경로

    private Integer status;         // HTTP 상태 코드

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    private String traceId;         // 추적 ID (선택사항)

    /**
     * 간단한 에러 응답 생성
     */
    public static ErrorResponse of(String message, int status) {
        return ErrorResponse.builder()
                .message(message)
                .status(status)
                .error(HttpStatus.valueOf(status).getReasonPhrase())
                .build();
    }

    /**
     * HttpStatus를 이용한 생성
     */
    public static ErrorResponse of(String message, HttpStatus status) {
        return ErrorResponse.builder()
                .message(message)
                .status(status.value())
                .error(status.getReasonPhrase())
                .build();
    }

}