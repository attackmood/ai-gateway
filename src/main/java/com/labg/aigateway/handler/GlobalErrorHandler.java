package com.labg.aigateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labg.aigateway.dto.response.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * packageName    : com.labg.aigateway.handler
 * fileName       : GlobalErrorHandler
 * author         : 이가은
 * date           : 2025-10-31
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-31          이가은             최초 생성
 */
@Slf4j
@Order(-2)  // 기본 에러 핸들러보다 우선순위 높게
@Component
@RequiredArgsConstructor
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 에러 로깅
        logError(exchange, ex);

        // 에러 응답 생성
        ErrorResponse errorResponse = createErrorResponse(exchange, ex);
        HttpStatus status = determineHttpStatus(ex);

        // HTTP 응답 설정
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // JSON 직렬화 및 응답
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("에러 응답 직렬화 실패", e);
            return exchange.getResponse().setComplete();
        }
    }

    /**
     * 에러 로깅
     */
    private void logError(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        if (ex instanceof IllegalArgumentException) {
            log.warn("잘못된 요청 - {} {}: {}", method, path, ex.getMessage());
        } else if (ex instanceof org.springframework.web.server.ResponseStatusException) {
            log.warn("응답 상태 예외 - {} {}: {}", method, path, ex.getMessage());
        } else {
            log.error("예외 발생 - {} {}", method, path, ex);
        }
    }

    /**
     * 에러 응답 생성
     */
    private ErrorResponse createErrorResponse(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status = determineHttpStatus(ex);
        String message = determineErrorMessage(ex);

        return ErrorResponse.builder()
                .success(false)
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .status(status.value())
                .build();
    }

    /**
     * HTTP 상태 코드 결정
     */
    private HttpStatus determineHttpStatus(Throwable ex) {
        // ResponseStatusException
        if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            return HttpStatus.resolve(rse.getStatusCode().value());
        }

        // IllegalArgumentException → 400 Bad Request
        if (ex instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }

        // NullPointerException → 500 Internal Server Error
        if (ex instanceof NullPointerException) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        // WebClientResponseException (AI Engine 호출 실패)
        if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException wcre) {
            return HttpStatus.resolve(wcre.getStatusCode().value());
        }

        // TimeoutException → 504 Gateway Timeout
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }

        // 기타 모든 에러 → 500
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * 사용자에게 보여줄 에러 메시지 결정
     */
    private String determineErrorMessage(Throwable ex) {
        // 4xx 에러: 사용자에게 구체적인 메시지
        if (ex instanceof IllegalArgumentException) {
            return ex.getMessage();
        }

        if (ex instanceof org.springframework.web.server.ResponseStatusException rse) {
            return rse.getReason() != null ? rse.getReason() : "요청 처리 중 오류가 발생했습니다";
        }

        // 5xx 에러: 일반적인 메시지 (상세 정보 노출 방지)
        if (ex instanceof java.util.concurrent.TimeoutException) {
            return "요청 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요";
        }

        if (ex instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
            return "외부 서비스 호출 중 오류가 발생했습니다";
        }

        // 기본 메시지 (스택 트레이스 노출 방지)
        return "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해주세요";
    }
}