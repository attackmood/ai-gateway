package com.labg.aigateway.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * packageName    : com.labg.aigateway.config
 * fileName       : JwtSecret
 * author         : 이가은
 * date           : 2025-11-05
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt.secret")
@Slf4j
public class JwtSecret {
    private String secretKey;
   	private long accessTokenExpTime;

	@PostConstruct
	public void init() {
		log.info("{} {}", secretKey, accessTokenExpTime);
	}
}
