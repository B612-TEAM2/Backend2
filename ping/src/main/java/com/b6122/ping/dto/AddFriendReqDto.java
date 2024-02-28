package com.b6122.ping.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddFriendReqDto {
    private String nickname;
    private String status;
    private Long toUserId;
}
