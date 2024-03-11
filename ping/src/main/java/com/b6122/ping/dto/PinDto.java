package com.b6122.ping.dto;

import com.b6122.ping.domain.Post;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

//pinMap 대신
@Getter@Setter
public class PinDto {
    Long id; //postId
    private float  latitude; //위도
    private float longitude; //경도
    public PinDto(Post post){
        this.id = post.getId();
        this.latitude = post.getLatitude();
        this.longitude = post.getLongitude();
    }
}
