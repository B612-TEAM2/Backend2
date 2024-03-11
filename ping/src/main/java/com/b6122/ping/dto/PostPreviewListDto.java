package com.b6122.ping.dto;
import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.PostScope;
import java.util.List;

// postPreviewList 대신
public class PostPreviewListDto {
    private Long id; //post id
    private String title; // 제목
    private PostScope scope; //공개 범위 [private, friends, public]
    private String createdDate; //생성 날짜
    private String contentPreview; //미리보기 15자

    //프론트로 이미지 파일 전달
    private byte[] imgByte;
    private byte[] userImg;
    private String userNickname;

    public PostPreviewListDto(Post post, List<String> postImageNames) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.scope = post.getScope();
        this.createdDate = post.getCreatedDate().toString();
        this.contentPreview = truncateContent(post.getContent(), 15); // Adjust for content preview
        if(!postImageNames.isEmpty()) {
            this.imgByte = post.getPostImgObjectBytes(postImageNames.get(0)); //대표 이미지 가져오기
        }
        if(post.getUser().getProfileImgObjectName() != null) {
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