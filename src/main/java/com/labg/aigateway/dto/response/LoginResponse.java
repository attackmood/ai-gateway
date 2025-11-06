package com.labg.aigateway.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * packageName    : com.labg.aigateway.dto.response
 * fileName       : LoginResponse
 * author         : 이가은
 * date           : 2025-11-06
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-06          이가은             최초 생성
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
}