package com.b6122.ping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResDto {

    private String nickname;
    private byte[] profileImg;
    private Long id;
}
