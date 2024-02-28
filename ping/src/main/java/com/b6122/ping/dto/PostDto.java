package com.b6122.ping.dto;

import com.b6122.ping.domain.Like;
import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.PostScope;
import com.b6122.ping.repository.LikeRepository;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import lombok.Getter;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;

//dto 분리,메모리 효율을 위해
@Getter @Setter
@RequiredArgsConstructor
public class PostDto {
    private  Long id; //post id

    private Long uid; //사용자

    private String location; //위치

    private float  latitude; //위도

    private float longitude; //경도

    private String title; // 제목

    private String content;

    private PostScope scope; //공개 범위 [private, friends, public]

    private int viewCount; // 조회수

    private int likeCount; // 좋아요 수

    private boolean myLike; //본인이 글에 좋아요 눌렀는지

    private LocalDateTime createdDate; //생성 날짜

    private LocalDateTime modifiedDate; //수정 날짜

    private String contentPreview; //미리보기 15자

    //프론트에서 이미지 파일 받을때

    private List<MultipartFile> imgs;
    private MultipartFile firstImg;

    private byte[] userImg;
    private String userNickname;


    //프론트로 이미지 파일 전달
    private byte[] imgByte;
    private List<byte[]> imgsByte;

    @OneToMany(mappedBy = "post")
    private List<Like> likes = new ArrayList<>();




    //pin- 위도, 경도,postId
    //Map위에 pin 보여주기
    public static PostDto pinMap(Post post) {
        PostDto postDto = new PostDto();
        postDto.setId(post.getId());
        postDto.setLongitude(post.getLongitude());
        postDto.setLatitude(post.getLatitude());
        return postDto;
    }

    //Home-Map 토글, pin클릭시 postPreview보여주기
    public static PostDto postPreviewMap(Post post) {
        PostDto postDto = new PostDto();
        postDto.setId(post.getId());
        postDto.setTitle(post.getTitle());
        postDto.setScope(post.getScope());
        postDto.setCreatedDate(post.getCreatedDate());
        postDto.setContentPreview(truncateContent(post.getContent(), 15)); // Adjust for content preview
        postDto.setImgByte(post.getByteArrayOfFirstImgByPath()); //대표 이미지 가져오기
        postDto.setUserImg(post.getUser().getProfileObjectImgBytes());
        postDto.setUserNickname(post.getUser().getNickname());
        return postDto;
    }


    //Home-List 토글
    public static PostDto postPreviewList(Post post, LikeRepository likeRepository) {
        PostDto postDto = new PostDto();
        postDto.setId(post.getId());
        postDto.setTitle(post.getTitle());
        postDto.setScope(post.getScope());
        postDto.setLikeCount(post.getLikeCount());
        postDto.setMyLike(likeRepository.checkMyLike(post.getId(), post.getUser().getId()));//사용자가 post에 좋아요 눌렀다면 myLike == True
        postDto.setCreatedDate(post.getCreatedDate());
        postDto.setContentPreview(truncateContent(post.getContent(), 15)); // Adjust for content preview
        postDto.setImgByte(post.getByteArrayOfFirstImgByPath()); //대표 이미지 가져오기
        postDto.setUserImg(post.getUser().getProfileObjectImgBytes());
        postDto.setUserNickname(post.getUser().getNickname());
        return postDto;
    }


    //글 보기
    public static PostDto postInfo(Post post, LikeRepository likeRepository) {
        PostDto postDto = new PostDto();
        postDto.setId(post.getId());
        postDto.setUid(post.getUser().getId());
        postDto.setTitle(post.getTitle());
        postDto.setScope(post.getScope());
        postDto.setLikeCount(post.getLikeCount());
        postDto.setMyLike(likeRepository.checkMyLike(post.getId(), post.getUser().getId()));//사용자가 post에 좋아요 눌렀다면 myLike == True
        postDto.setCreatedDate(post.getCreatedDate());
        postDto.setCreatedDate(post.getModifiedDate());
        postDto.setContent(post.getContent());
        postDto.setImgsByte(post.getByteArraysOfImgsByPaths()); //모든 이미지 반환
        postDto.setUserImg(post.getUser().getProfileObjectImgBytes());
        postDto.setUserNickname(post.getUser().getNickname());
        return postDto;
    }


    private static String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        } else {
            return content.substring(0, maxLength) + "...";
        }
    }
}