package com.b6122.ping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.atomic.AtomicBoolean;

@Data
@AllArgsConstructor
public class OauthLoginUserDto {
    private Long id;
    private String username;
    AtomicBoolean wasMember;
}

