package com.labg.aigateway.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * packageName    : com.labg.aigateway.dto
 * fileName       : UserDto
 * author         : 이가은
 * date           : 2025-11-05
 * description    :
 * ===========================================================
 * DATE                 AUTHOR              NOTE
 * -----------------------------------------------------------
 * 2025-11-05          이가은             최초 생성
 */
@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    @JsonAlias("_id")
    private String id;
    private String email;
    private String password;
    private String username;
    private Date createdAt;

}
