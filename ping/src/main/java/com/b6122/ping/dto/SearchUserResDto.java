package com.b6122.ping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchUserResDto {

    private String nickname;
    private byte[] profileImg;
    private boolean isFriend;
}
