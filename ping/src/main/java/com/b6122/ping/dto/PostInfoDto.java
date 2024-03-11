package com.b6122.ping.dto;
import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.PostScope;
import com.b6122.ping.repository.LikeRepository;
import java.util.List;

public class PostInfoDto {
    private  Long id; //post id

    private Long uid; //사용자

    private String title; // 제목

    private String content;

    private PostScope scope; //공개 범위 [private, friends, public]

    private int likeCount; // 좋아요 수

    private boolean myLike; //본인이 글에 좋아요 눌렀는지

    private String createdDate; //생성 날짜

    private String modifiedDate; //수정 날짜


    //프론트로 이미지 파일 전달
    private List<byte[]> imgsByte;
    private byte[] userImg;

    private String userNickname;

    public PostInfoDto(Post post, LikeRepository likeRepository, List<String> postImageNames) {

        this.id = post.getId();
        this.uid = post.getUser().getId();
        this.title = post.getTitle();
        this.scope = post.getScope();
        this.likeCount = post.getLikeCount();
        this.myLike = likeRepository.checkMyLike(post.getId(), post.getUser().getId());
        this.createdDate = post.getCreatedDate().toString();
        this.modifiedDate = post.getModifiedDate().toString();
        this.content = post.getContent();

        if (!(postImageNames.isEmpty())) {
            this.imgsByte = post.getPostImgObjectsBytes(postImageNames); //모든 이미지 가져오기
        }
        if (post.getUser().getProfileImgObjectName() != null){
            this.userImg = post.getUser().getProfileObjectImgBytes();
        }
        this.userNickname = post.getUser().getNickname();
    }
}
