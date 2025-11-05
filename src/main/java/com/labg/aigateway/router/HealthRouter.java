package com.labg.aigateway.router;

import com.labg.aigateway.handler.HealthHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * packageName    : com.labg.aigateway.router
 * fileName       : HealthRouter
 * author         : 이가은
 * date           : 2025-11-05
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */
@Configuration
@AllArgsConstructor
public class HealthRouter {

    private final HealthHandler healthHandler;

    @Bean
    public RouterFunction<ServerResponse> healthRoutes() {
        return RouterFunctions.route()
                .GET("/api/health", healthHandler::handleHealthCheck)
                .build();
    }
}
