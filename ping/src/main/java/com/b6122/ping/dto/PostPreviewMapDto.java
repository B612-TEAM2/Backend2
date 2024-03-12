package com.b6122.ping.dto;

import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.PostScope;
import lombok.Data;
import java.util.List;

@Data
public class PostPreviewMapDto {
    private Long id;
    private String title;
    private PostScope scope;
    private String createdDate; //생성 날짜

    private String contentPreview; //미리보기 15자
    private byte[] imgByte;
    private byte[] userImg;
    private String userNickname;
    public PostPreviewMapDto(Post post, List<String> postImageNames ){
        this.id = post.getId();
        this.title = post.getTitle();
        this.scope = post.getScope();
        this.contentPreview = truncateContent(post.getContent(), 15);
        this.createdDate = post.getCreatedDate().toString();

        if(!postImageNames.isEmpty()) {
            this.imgByte = post.getPostImgObjectBytes(postImageNames.get(0)); //대표 이미지 가져오기
        }
        if(post.getUser().getProfileImgObjectName() != null){
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
