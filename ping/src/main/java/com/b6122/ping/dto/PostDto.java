package com.b6122.ping.dto;


import com.b6122.ping.domain.PostScope;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;


//dto 분리,메모리 효율을 위해
@Getter @Setter
@RequiredArgsConstructor
public class PostDto {
    private Long id; //post id

    private Long uid; //사용자

    private String location; //위치

    private float latitude; //위도

    private float longitude; //경도

    private String title; // 제목

    private String content;

    private PostScope scope; //공개 범위 [private, friends, public]

}