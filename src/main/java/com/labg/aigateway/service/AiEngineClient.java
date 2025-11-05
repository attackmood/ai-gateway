package com.labg.aigateway.service;

import com.labg.aigateway.dto.request.AiEngineRequest;
import com.labg.aigateway.dto.response.AiResponse;
import com.labg.aigateway.dto.response.HealthResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : AiEngineClient
 * author         : 이가은
 * date           : 2025-10-28
 * description    : Python AI Engine과 통신
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class AiEngineClient {

    private final WebClient webClient;
    @Value("${ai-engine.timeout}")
    Duration timeout;
    @Value("${ai-engine.retry.max-attempts:3}")
    private int maxAttempts;
    @Value("${ai-engine.retry.backoff:1s}")
    private Duration backoff;


    /**
     * Python AI Engine에 쿼리 전송
     */
    @CircuitBreaker(name = "aiEngine", fallbackMethod = "queryFallback")
    @Retry(name = "aiEngine")
    public Mono<AiResponse> query(AiEngineRequest request) {
        log.debug("AI Engine 요청 - sessionId: {}, message length: {}",
                  request.getSessionId(), request.getMessage().length());

        return webClient.post()
                .uri("/api/chat/query")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiResponse.class)
                .timeout(timeout)
                .doOnSuccess(response ->
                    log.info("AI Engine 응답 성공 - sessionId: {}, processingTime: {}s", response.getSessionId(), response.getProcessingTime())
                )
                .doOnError(error ->
                    log.error("AI Engine 요청 실패 - sessionId: {}, error: {}", request.getSessionId(), error.getMessage())
                );
    }


    private Mono<AiResponse> queryFallback(AiEngineRequest request, Exception exception) {
        log.warn("Circuit Breaker Fallback 실행 - sessionId: {}, error: {}",
                request.getSessionId(), exception.getMessage());

        return Mono.just(AiResponse.builder()
                .success(false)
                .message("현재 AI 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .sessionId(request.getSessionId())
                .processingTime(0.0)
                .modeUsed("fallback")
                .build());
    }

    /**
     * AI Engine 헬스체크
     * Python API의 구조화된 헬스체크 응답을 받아서 반환
     *
     * @return HealthResponse (상태, 서비스 정보 등 포함)
     */
    public Mono<HealthResponse> healthCheck() {
        log.debug("AI Engine 헬스체크 시작");

        return webClient.get()
                .uri("/api/health/")  // trailing slash 포함 (리다이렉트 방지)
                .retrieve()
                .bodyToMono(HealthResponse.class)
                .doOnSuccess(response -> 
                    log.debug("AI Engine 헬스체크 성공 - status: {}, routerAvailable: {}, uptime: {}", 
                            response.getStatus(), response.getRouterAvailable(), response.getUptime())
                )
                .timeout(Duration.ofSeconds(5))
                .doOnError(error -> 
                    log.error("AI Engine 헬스체크 실패 - error: {}", error.getMessage())
                )
                .onErrorResume(error -> {
                    // 에러 발생 시 unhealthy 상태로 반환
                    return Mono.just(HealthResponse.builder()
                            .status("unhealthy")
                            .service("Smart-RAG Chat")
                            .version("unknown")
                            .routerAvailable(false)
                            .uptime("0")
                            .build());
                });
    }

    /**
     * 의도 분석만 수행 (내부용 엔드포인트)
     *
     * @param text 분석할 텍스트
     * @return 의도 분석 결과
     */
    public Mono<String> analyzeIntent(String text) {
        log.debug("의도 분석 요청 - text length: {}", text.length());

        return webClient.post()
                .uri("/api/v1/internal/analyze")
                .bodyValue(text)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(result ->log.debug("의도 분석 성공 - result: {}", result))
                .onErrorResume(error -> {
                    log.error("의도 분석 실패", error);
                    return Mono.just("unknown");
                });
    }


    /**
     * 임베딩 생성 (내부용 엔드포인트)
     *
     * @param texts 임베딩할 텍스트 목록
     * @return 임베딩 벡터
     */
    public Mono<double[][]> generateEmbeddings(String[] texts) {
        log.debug("임베딩 생성 요청 - texts count: {}", texts.length);

        return webClient.post()
                .uri("/api/v1/internal/embed")
                .bodyValue(texts)
                .retrieve()
                .bodyToMono(double[][].class)
                .timeout(Duration.ofSeconds(15))
                .doOnSuccess(embeddings ->log.debug("임베딩 생성 성공 - count: {}", embeddings.length))
                .onErrorResume(error -> {
                    log.error("임베딩 생성 실패", error);
                    return Mono.empty();
                });
    }


}
