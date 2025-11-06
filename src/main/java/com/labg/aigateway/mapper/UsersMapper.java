package com.labg.aigateway.mapper;

import com.labg.aigateway.entity.Users;
import com.labg.aigateway.dto.UserDto;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface UsersMapper {

    Users toUser(UserDto userDto);

    UserDto toDto(Users users);

    Users toEntity(UserDto usersDTO);

    List<UserDto> toDtoList(List<Users> users);



    Map<String, UserDto> toDtoMap(Map<String, Users> users);

}
