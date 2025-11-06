package com.labg.aigateway.router;

import com.labg.aigateway.handler.AuthHandler;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * packageName    : com.labg.aigateway.router
 * fileName       : AuthRouter
 * author         : 이가은
 * date           : 2025-11-06
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-06          이가은             최초 생성
 */
@Configuration
@AllArgsConstructor
public class AuthRouter {
    private final AuthHandler authHandler;

    @Bean
    public RouterFunction<ServerResponse> authRoutes() {
        return RouterFunctions.route()
                .POST("/api/auth/register", RequestPredicates.accept(MediaType.APPLICATION_JSON), authHandler::register)
                .POST("/api/auth/login", RequestPredicates.accept(MediaType.APPLICATION_JSON), authHandler::login)
                .build();
    }
}
