package com.labg.aigateway.service;

import com.labg.aigateway.dto.response.LoginResponse;
import com.labg.aigateway.entity.Users;
import reactor.core.publisher.Mono;

/**
 * packageName    : com.labg.aigateway.service
 * fileName       : AuthKeysService
 * author         : 이가은
 * date           : 2025-11-05
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */
public interface AuthService {

    Mono<LoginResponse> register(Users users);

    Mono<LoginResponse> login(Users users);
}
