package com.labg.aigateway.config;

/**
 * packageName    : com.labg.aigateway.config
 * fileName       : WebClientConfig
 * author         : 이가은
 * date           : 2025-10-28
 * description    : WebClient 설정
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Configuration
@Slf4j
public class WebClientConfig {
    @Value("${ai-engine.base-url}")
    String baseUrl;
    @Value("${ai-engine.timeout}")
    Duration timeout;
    @Value("${ai-engine.retry.max-attempts:3}")
    private int maxAttempts;
    @Value("${ai-engine.retry.backoff:1s}")
    private Duration backoff;


    @Bean
    public WebClient webClient() {
        // Netty HttpClient 설정
        HttpClient httpClient = HttpClient.create()
                // 연결 타임아웃 (10초)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                // Response 타임아웃 (30초)
                .responseTimeout(Duration.ofSeconds(30))
                // Read/Write 타임아웃
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }


    /**
     * 요청 로깅 필터
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("Request: {} {}", request.method(), request.url());
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("Request Header: {}={}", name, value))
            );
            return Mono.just(request);
        });
    }

    /**
     * 응답 로깅 필터
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.debug("Response Status: {}", response.statusCode());
            response.headers().asHttpHeaders().forEach((name, values) ->
                    values.forEach(value -> log.debug("Response Header: {}={}", name, value))
            );
            return Mono.just(response);
        });
    }
}
