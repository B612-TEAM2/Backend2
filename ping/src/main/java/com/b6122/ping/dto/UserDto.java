package com.b6122.ping.dto;

import com.b6122.ping.domain.UserRole;
import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    public UserDto(Long id,String username) {
        this.id = id;
        this.username = username;
    }
}
