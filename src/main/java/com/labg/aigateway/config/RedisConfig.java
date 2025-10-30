package com.labg.aigateway.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
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

            // LocalDateTime 직렬화 지원을 위해 JavaTimeModule 등록
            ObjectMapper objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .activateDefaultTyping(
                            BasicPolymorphicTypeValidator.builder()
                                    .allowIfSubType(Object.class)
                                    .build(),
                            ObjectMapper.DefaultTyping.NON_FINAL,
                            JsonTypeInfo.As.PROPERTY
                    )
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Jackson2JsonRedisSerializer 생성
            Jackson2JsonRedisSerializer<Object> serializer =
                    new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

            // RedisSerializationContext 설정
            RedisSerializationContext<String, Object> serializationContext =
                    RedisSerializationContext.<String, Object>newSerializationContext()
                            .key(new StringRedisSerializer())
                            .value(serializer)
                            .hashKey(new StringRedisSerializer())
                            .hashValue(serializer)
                            .build();

            return new ReactiveRedisTemplate<>(factory, serializationContext);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create ReactiveRedisTemplate", e);
        }
    }
}
