package com.labg.aigateway.handler;

import com.labg.aigateway.service.AiEngineClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * packageName    : com.labg.aigateway.handler
 * fileName       : HealthHandler
 * author         : 이가은
 * date           : 2025-11-05
 * description    : 
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthHandler {
    
    private final AiEngineClient aiEngineClient;
    
    /**
     * 헬스체크 핸들러
     * AI Engine 상태를 확인하고 Gateway 자체 상태도 함께 반환
     *
     * @param request ServerRequest
     * @return ServerResponse with health status
     */
    public Mono<ServerResponse> handleHealthCheck(ServerRequest request) {
        return aiEngineClient.healthCheck()
                .flatMap(aiEngineHealth -> {
                    // AI Engine이 healthy이면 OK, 그 외에는 SERVICE_UNAVAILABLE
                    HttpStatus status = aiEngineHealth.isHealthy() 
                            ? HttpStatus.OK 
                            : HttpStatus.SERVICE_UNAVAILABLE;
                    
                    return ServerResponse.status(status)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(aiEngineHealth);
                })
                .onErrorResume(error -> {
                    log.error("헬스체크 실패", error);
                    Map<String, Object> errorStatus = new HashMap<>();
                    errorStatus.put("status", "DOWN");
                    errorStatus.put("gateway", "healthy");
                    errorStatus.put("aiEngine", Map.of(
                            "status", "unreachable",
                            "error", error.getMessage() != null ? error.getMessage() : "Unknown error"
                    ));
                    
                    return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(errorStatus);
                });
    }
}