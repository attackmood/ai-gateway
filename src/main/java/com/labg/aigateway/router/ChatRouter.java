package com.labg.aigateway.router;

import com.labg.aigateway.handler.ChatHandler;
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
 * fileName       : ChatRouter
 * author         : 이가은
 * date           : 2025-10-28
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Configuration
@AllArgsConstructor
public class ChatRouter {

    private ChatHandler chatHandler;

    @Bean
    public RouterFunction<ServerResponse> chatRoutes() {
        return RouterFunctions.route()
                .POST("/api/chat/query", RequestPredicates.accept(MediaType.APPLICATION_JSON), chatHandler::handleChat)
                .build();
    }
}
