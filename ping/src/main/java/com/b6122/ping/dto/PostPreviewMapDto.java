package com.b6122.ping.dto;

import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.PostScope;

import java.time.LocalDateTime;
import java.util.List;

public class PostPreviewMapDto {
    private Long id;
    private String title;
    private PostScope scope;
    private LocalDateTime createdDate; //생성 날짜
    private byte[] imgByte;
    private byte[] userImg;
    private String userNickname;
    private String contentPreview; //미리보기 15자

    public PostPreviewMapDto(Post post){
        this.id = post.getId();
        this.title = post.getTitle();
        this.scope = post.getScope();
        this.contentPreview = truncateContent(post.getContent(), 15);

        if(!(post.getPostImgObjectsName().isEmpty())) {
            this.imgByte = post.getPostImgObjectBytes(post.getPostImgObjectsName().get(0)); //대표 이미지 가져오기
        }
        if(!(post.getUser().getProfileImgObjectName().isEmpty())) {
            this.userImg = post.getUser().getProfileObjectImgBytes();
        }
        this.userNickname = post.getUser().getNickname();
    }

    private static String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        } else {
            return content.substring(0, maxLength) + "...";
        }
    }
}
