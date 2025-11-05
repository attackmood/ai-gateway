package com.labg.aigateway.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * packageName    : com.labg.aigateway.dto.response
 * fileName       : HealthResponse
 * author         : 이가은
 * date           : 2025-11-05
 * description    : Python AI Engine의 헬스체크 응답 DTO
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthResponse {
    
    /**
     * 상태: "healthy", "degraded", "unhealthy"
     */
    private String status;
    
    /**
     * 서비스 이름
     */
    private String service;
    
    /**
     * 버전 정보
     */
    private String version;
    
    /**
     * 라우터 사용 가능 여부
     */
    @JsonProperty("router_available")
    private Boolean routerAvailable;
    
    /**
     * 업타임 정보
     */
    private String uptime;
    
    /**
     * 헬스체크가 정상인지 확인
     * 
     * @return 정상이면 true
     */
    public boolean isHealthy() {
        return "healthy".equalsIgnoreCase(status);
    }
    
    /**
     * 헬스체크가 비정상인지 확인
     * 
     * @return 비정상이면 true
     */
    public boolean isUnhealthy() {
        return "unhealthy".equalsIgnoreCase(status);
    }
    
    /**
     * 헬스체크가 저하 상태인지 확인
     * 
     * @return 저하 상태이면 true
     */
    public boolean isDegraded() {
        return "degraded".equalsIgnoreCase(status);
    }
}

