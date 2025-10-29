package com.labg.aigateway.service;

import com.labg.aigateway.dto.request.AiEngineRequest;
import com.labg.aigateway.dto.response.AiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

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
//    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${ai-engine.timeout}")
    Duration timeout;
    @Value("${ai-engine.retry.max-attempts:3}")
    private int maxAttempts;
    @Value("${ai-engine.retry.backoff:1s}")
    private Duration backoff;


    /**
     * Python AI Engine에 쿼리 전송
     */
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
                )
                .onErrorResume(this::handleError);
    }

    /**
     * AI Engine 헬스체크
     * @return
     */
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
     *
     * @return 정상 여부
     */
    public Mono<Boolean> healthCheck() {
        log.debug("AI Engine 헬스체크 시작");

        return webClient.get()
                .uri("/api/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("AI Engine 헬스체크 성공 - response: {}", response);
                    return true;
                })
                .timeout(Duration.ofSeconds(5))
                .doOnError(error ->log.error("AI Engine 헬스체크 실패 - error: {}", error.getMessage()))
                .onErrorReturn(false);
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







    /**
     * 에러 처리 및 Fallback 응답
     */
    /**
     * 에러 처리 및 복구
     *
     * @param error 발생한 에러
     * @return 복구된 응답 또는 에러
     */
    private Mono<AiResponse> handleError(Throwable error) {
        if (error instanceof WebClientResponseException webClientError) {
            log.error("WebClient 에러 - status: {}, body: {}",
                     webClientError.getStatusCode(),
                     webClientError.getResponseBodyAsString());

            return Mono.just(createErrorResponse(
                "AI 서비스 응답 오류",
                webClientError.getStatusCode().value()
            ));
        }

        if (error instanceof java.util.concurrent.TimeoutException) {
            log.error("AI Engine 타임아웃 - timeout: {}s", timeout.getSeconds());
            return Mono.just(createErrorResponse("응답 시간이 초과되었습니다", 408));
        }

        if (error instanceof AiEngineException aiError) {
            log.error("AI Engine 커스텀 에러 - message: {}", aiError.getMessage());
            return Mono.just(createErrorResponse(aiError.getMessage(), 500));
        }

        // 알 수 없는 에러
        log.error("알 수 없는 에러", error);
        return Mono.just(createErrorResponse(
            "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
            500
        ));
    }

    /**
     * 에러 응답 생성
     *
     * @param message 에러 메시지
     * @param statusCode HTTP 상태 코드
     * @return 에러 응답
     */
    private AiResponse createErrorResponse(String message, int statusCode) {
        return AiResponse.builder()
                .success(false)
                .message(message)
                .processingTime(0.0)
                .modeUsed("error")
                .metadata(new AiResponse.Metadata())
                .build();
    }

    /**
     * AI Engine 커스텀 예외
     */
    public static class AiEngineException extends RuntimeException {
        public AiEngineException(String message) {
            super(message);
        }

        public AiEngineException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
