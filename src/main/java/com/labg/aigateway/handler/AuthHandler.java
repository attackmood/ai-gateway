package com.labg.aigateway.handler;

import com.labg.aigateway.dto.UserDto;
import com.labg.aigateway.mapper.UsersMapper;
import com.labg.aigateway.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * packageName    : com.labg.aigateway.handler
 * fileName       : AuthHandler
 * author         : 이가은
 * date           : 2025-11-06
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-06          이가은             최초 생성
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHandler {
    private final AuthService authService;
    private final UsersMapper usersMapper;

    /**
     * 회원가입
     */
    public Mono<ServerResponse> register(ServerRequest request) {
        return request.bodyToMono(UserDto.class)
                .map(usersMapper::toEntity)
                .flatMap(authService::register)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest()
                                .bodyValue(errorResponse(e.getMessage()))
                );
    }

    /**
     * 로그인
     * 로그인 성공 시 access_token 쿠키를 설정하여 응답
     */
    public Mono<ServerResponse> login(ServerRequest request) {
        return request.bodyToMono(UserDto.class)
                .map(usersMapper::toEntity)
                .flatMap(authService::login)
                .flatMap(response -> {
                    // access_token 쿠키 생성 (7일 유효)
                    ResponseCookie cookie = ResponseCookie.from("access_token", response.getToken())
                            .httpOnly(false)  // JavaScript에서 접근 가능 (클라이언트 인증 확인용)
                            .secure(false)    // HTTPS에서만 전송 (개발 환경에서는 false)
                            .path("/")        // 모든 경로에서 사용 가능
                            .maxAge(60 * 60)
                            .sameSite("Strict")   // CSRF 방지
                            .build();

                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .cookie(cookie)  // 쿠키 설정
                            .bodyValue(response);
                })
                .onErrorResume(IllegalArgumentException.class, e ->
                        ServerResponse.badRequest()
                                .bodyValue(errorResponse(e.getMessage()))
                );
    }

    private Object errorResponse(String message) {
        return new ErrorResponse(false, "Authentication Error", message);
    }

    record ErrorResponse(boolean success, String error, String message) {}
}
