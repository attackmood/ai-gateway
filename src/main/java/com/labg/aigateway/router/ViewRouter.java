package com.labg.aigateway.router;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * packageName    : com.labg.aigateway.router
 * fileName       : ViewRouter
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
public class ViewRouter {

    @Bean
    public RouterFunction<ServerResponse> viewRoutes() {
        return RouterFunctions.route()
                // GET / 또는 /index → templates/index.html 렌더링 (Thymeleaf)
                .GET("/", RequestPredicates.accept(MediaType.TEXT_HTML), request ->
                        ServerResponse.ok().render("index", Map.of(
                                "title", "AI Gateway",
                                "description", "WebFlux + Thymeleaf"
                        ))
                )
                .GET("/index", RequestPredicates.accept(MediaType.TEXT_HTML), request ->
                        ServerResponse.ok().render("index", Map.of(
                                "title", "AI Gateway",
                                "description", "WebFlux + Thymeleaf"
                        ))
                )
                .build();
    }
}
