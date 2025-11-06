package com.labg.aigateway.repository;

import com.labg.aigateway.entity.QueryHistory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * packageName    : com.labg.aigateway.repository
 * fileName       : QueryHistoryRepository
 * author         : 이가은
 * date           : 2025-10-28
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
public interface QueryHistoryRepository extends ReactiveMongoRepository<QueryHistory, String> {
}
