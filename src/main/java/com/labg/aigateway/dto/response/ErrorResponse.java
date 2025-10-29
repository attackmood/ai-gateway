package com.labg.aigateway.dto.response;

import lombok.*;

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
public class ErrorResponse {

    private boolean success = false;

    private String error;

    private String message;

    private String path;

    private LocalDateTime timestamp;

    private Integer status;

    public static ErrorResponse of(String error, String message, String path, int status) {
        return ErrorResponse.builder()
                .error(error)
                .message(message)
                .path(path)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
