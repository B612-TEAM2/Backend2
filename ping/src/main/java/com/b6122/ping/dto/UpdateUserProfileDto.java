package com.b6122.ping.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class UpdateUserProfileDto {

    private Long id;
    private String nickname;
    private String profileImgObjectName;
}
