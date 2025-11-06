package com.labg.aigateway.repository;

import com.labg.aigateway.entity.Users;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * packageName    : com.labg.aigateway.repository
 * fileName       : UsersRepository
 * author         : 이가은
 * date           : 2025-11-06
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-06          이가은             최초 생성
 */
@Repository
public interface UsersRepository extends ReactiveMongoRepository<Users, String> {
    Mono<Users> findByUsername(String username);

    Mono<Boolean> existsByUsername(String username);


}
