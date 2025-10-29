package com.labg.aigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * packageName    : com.labg.aigateway.config
 * fileName       : RedisConfig
 * author         : 이가은
 * date           : 2025-10-28
 * description    : ReactiveRedisTemplate 직렬화 설정 및 Bean 등록
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-10-28          이가은             최초 생성
 */
@Configuration
public class RedisConfig {

    /**
     * ReactiveRedisTemplate<String, Object> Bean 등록.
     * - key/hashKey: String 직렬화
     * - value/hashValue: GenericJackson2Json 직렬화
     *
     * @param factory Reactive Redis 커넥션 팩토리 (Lettuce 기반 자동 구성 사용)
     * @return ReactiveRedisTemplate<String, Object>
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        try {
            // Key는 String으로 직렬화
            StringRedisSerializer keySerializer = new StringRedisSerializer();

            GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

            RedisSerializationContext<String, Object> context =
                    RedisSerializationContext.<String, Object>newSerializationContext(keySerializer)
                            .key(keySerializer)
                            .value(valueSerializer)
                            .hashKey(keySerializer)
                            .hashValue(valueSerializer)
                            .build();

            return new ReactiveRedisTemplate<>(factory, context);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create ReactiveRedisTemplate", e);
        }
    }
}
