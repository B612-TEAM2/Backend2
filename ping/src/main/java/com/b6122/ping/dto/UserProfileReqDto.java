package com.b6122.ping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class UserProfileReqDto {

    private String nickname;
    private MultipartFile profileImg;
    private Long id;
}
