package com.labg.aigateway.service.impl;

import com.labg.aigateway.dto.response.LoginResponse;
import com.labg.aigateway.entity.Users;
import com.labg.aigateway.repository.UsersRepository;
import com.labg.aigateway.service.AuthService;
import com.labg.aigateway.service.JwtService;
import com.labg.aigateway.util.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * packageName    : com.labg.aigateway.service.impl
 * fileName       : AuthKeysServiceImpl
 * author         : 이가은
 * date           : 2025-11-05
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UsersRepository usersRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Override
    public Mono<LoginResponse> register(Users users) {
        return usersRepository.existsByUsername(users.getUsername())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("이미 존재하는 사용자명입니다"));
                    }

                    users.setPassword(passwordEncoder.encode(users.getPassword()));
                    return usersRepository.save(users);
                })
                .map(user -> {
                    String token = jwtService.generateToken(user.getId(), user.getUsername());
                    log.info("회원가입 성공 - username: {}", user.getUsername());
                    return new LoginResponse(token, user.getUsername());
                });
    }

    /**
     * 로그인
     */
    @Override
    public Mono<LoginResponse> login(Users users) {
        return usersRepository.findByUsername(users.getUsername())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(users.getPassword(), user.getPassword())) {
                        return Mono.error(new IllegalArgumentException("비밀번호가 일치하지 않습니다"));
                    }

                    // 마지막 로그인 시간 업데이트
                    user.setLastLoginAt(LocalDateTime.now());
                    return usersRepository.save(user);
                })
                .map(user -> {
                    String token = jwtService.generateToken(user.getId(), user.getUsername());
                    log.info("로그인 성공 - username: {}", user.getUsername());
                    return new LoginResponse(token, user.getUsername());
                });
    }

}
