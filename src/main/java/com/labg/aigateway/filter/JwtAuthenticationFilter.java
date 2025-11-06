package com.labg.aigateway.filter;

import com.labg.aigateway.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * packageName    : com.labg.aigateway.filter
 * fileName       : JwtAuthenticationFilter
 * author         : 이가은
 * date           : 2025-11-06
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-06          이가은             최초 생성
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtService jwtService;

    // 인증이 필요 없는 경로
    private static final String[] PUBLIC_PATHS = {
            // API 엔드포인트
            "/api/auth/login",
            "/api/auth/register",
            "/api/health",
            // 뷰 페이지 (프론트엔드에서 토큰 확인 후 리다이렉트)
            "/",
            "/index",
            "/login",
            "/register",
            // 정적 리소스
            "/css",
            "/js",
            "/static",
            "/favicon.ico"
    };


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Public 경로는 인증 스킵
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 쿠키에서 access_token 추출
        String token = null;
        var cookies = exchange.getRequest().getCookies().get("access_token");
        if (cookies != null && !cookies.isEmpty()) {
            token = cookies.get(0).getValue();
        }

        if (token == null || token.isEmpty()) {
            log.warn("인증 토큰 없음 (쿠키에서 access_token을 찾을 수 없음) - path: {}", path);
            return unauthorized(exchange, "인증 토큰이 필요합니다");
        }

        // 토큰 검증
        if (!jwtService.validateToken(token)) {
            log.warn("유효하지 않은 토큰 - path: {}", path);
            return unauthorized(exchange, "유효하지 않은 토큰입니다");
        }

        // 토큰에서 사용자 정보 추출
        String userId = jwtService.extractUserId(token);
        String username = jwtService.extractUsername(token);

        // Request에 사용자 정보 추가 (Handler에서 사용 가능)
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", userId)
                .header("X-Username", username)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();

        log.debug("인증 성공 - userId: {}, username: {}, path: {}", userId, username, path);

        return chain.filter(mutatedExchange);
    }

    /**
     * Public 경로 확인
     * - 루트 경로("/")는 정확히 일치할 때만 public
     * - 정적 리소스("/css", "/js" 등)는 startsWith로 확인
     * - API 경로는 정확히 일치할 때만 public
     */
    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            // 루트 경로는 정확히 일치해야 함
            if ("/".equals(publicPath)) {
                if ("/".equals(path)) {
                    return true;
                }
            } else {
                // 나머지 경로는 startsWith로 확인
                if (path.startsWith(publicPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 401 Unauthorized 응답
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String json = String.format("{\"success\":false,\"error\":\"Unauthorized\",\"message\":\"%s\"}", message);
        byte[] bytes = json.getBytes();

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    private boolean targetURI(String path) {
        return path.startsWith("/api/chat") || path.startsWith("/api/goals") || path.startsWith("/api/emp") || path.startsWith("/api/dept") || path.startsWith("/api/menu");
    }
}