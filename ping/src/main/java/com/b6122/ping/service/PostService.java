package com.b6122.ping.service;

import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.PostImage;
import com.b6122.ping.domain.User;
import com.b6122.ping.dto.PostDto;
import com.b6122.ping.repository.LikeRepository;
import com.b6122.ping.repository.PostRepository;
import com.b6122.ping.repository.datajpa.PostImageDataRepository;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final UserDataRepository userDataRepository;
    private final PostImageDataRepository postImageDataRepository;

    @Transactional
    public Long createPost(PostDto postDto, List<MultipartFile> imgs) {
        Post post;
        post = new Post();
        User user = userDataRepository.findById(postDto.getUid()).orElseThrow(RuntimeException::new);
        post.setUser(user);
        post.setLocation(postDto.getLocation());
        post.setLatitude(postDto.getLatitude());
        post.setLongitude(postDto.getLongitude());
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setScope(postDto.getScope());
        post.setViewCount(postDto.getViewCount());
        post.setLikeCount(postDto.getLikeCount());
        post.setLikes(postDto.getLikes());
        Long id = postRepository.save(post);

        if (imgs != null) {
            List<String> objectNameList = post.putImgs(imgs);
            for (String objectName : objectNameList) {
                PostImage postImage = PostImage.createPostImage(post, objectName);
                postImageDataRepository.save(postImage);
            }
        }
        return id;
    }

    //post 수정
    @Transactional
    public Long modifyPost(PostDto postDto, List<MultipartFile> imgs) throws IOException {

        Post post = postRepository.findById(postDto.getId());
        post.setId(postDto.getId());
        post.setLocation(postDto.getLocation());
        post.setLatitude(postDto.getLatitude());
        post.setLongitude(postDto.getLongitude());
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());
        post.setScope(postDto.getScope());
        if (imgs != null) {
            //수정 전 이미지 파일을 Ncp Object Storage에서 삭제
            List<PostImage> postImageEntities = postImageDataRepository.findByPostId(post.getId());
            List<String> postImageNames = new ArrayList<>();
            for (PostImage postImageEntity : postImageEntities) {
                postImageNames.add(postImageEntity.getPostImageName());
            }
            post.deletePostImgObjectsInStorage(postImageNames);

            //NCP, 및 db에 저장
            List<String> objectNameList = post.putImgs(imgs);
            for (String objectName : objectNameList) {
                PostImage postImage = PostImage.createPostImage(post, objectName);
                postImageDataRepository.save(postImage);
            }
        }
        return post.getId();
    }


    //글 삭제
    public void deletePost(Long pid) throws IOException {
        Post post = postRepository.findById(pid);
        List<PostImage> postImageEntities = postImageDataRepository.findByPostId(post.getId());
        List<String> postImageNames = new ArrayList<>();
        for (PostImage postImageEntity : postImageEntities) {
            postImageNames.add(postImageEntity.getPostImageName());
        }
        post.deletePostImgObjectsInStorage(postImageNames);
        postRepository.deletePost(pid);
    }


    //글 전체보기 요청
    public PostDto getPostInfo(Long pid, Long uid) {
        Post post = postRepository.findById(pid);

        if (post.getUser().getId().equals(uid)) {//사용자와 글 작성자와 다른 경우만 viewCount++
            postRepository.updateViewCount(post.getViewCount() + 1, post.getId());
        }
        List<PostImage> postImageEntities = postImageDataRepository.findByPostId(post.getId());
        List<String> postImageNames = new ArrayList<>();
        for (PostImage postImageEntity : postImageEntities) {
            postImageNames.add(postImageEntity.getPostImageName());
        }
        return PostDto.postInfo(post, likeRepository, postImageNames);
    }

    //for문 마다 확인하기엔 통신이 너무 오래 걸림, db에서 한번에 검사하는게 효율적
    //postid, MyLike 받아서 체크
    public void toggleLike(List<Long> pids, List<Boolean> myLikes, Long uid) {
        List<Long> delLikePids = new ArrayList<>();
        List<Long> insertLikePids = new ArrayList<>();
        for (int i = 0; i < pids.size(); i++) {
            if (!myLikes.get(i)) {
                delLikePids.add(pids.get(i));
            } else {
                insertLikePids.add(pids.get(i));
            }
        }
        likeRepository.delete(delLikePids, uid);
        likeRepository.save(insertLikePids, uid);
    }

    //pin 클릭시 해당 위치의 postList 반환,home friend public 동일한 함수 사용
    public List<PostDto> getPostsPreviewPin(List<Long> pids) {
        List<Post> posts = new ArrayList<>();
        for (Long pid : pids) {
            posts.add(postRepository.findById(pid));
        }
        return getPostPreviewMapDtos(posts);
    }

    //Home-Map 클릭 전, 내가 작성한 모든 글의 pin띄우기
    public List<PostDto> getPinsHomeMap(Long uid) {
        List<Post> posts = postRepository.findByUid(uid);
        return posts.stream().map(PostDto::pinMap).collect(Collectors.toList());
    }

    //Home-List 토글, postList 반환
    public List<PostDto> getPostsHomeList(Long uid) {
        List<Post> posts = postRepository.findByUid(uid);
        return getPostPreviewListDtos(posts);
    }

    //Friends
    //친구가 작성한 글의 pin 반환
    public List<PostDto> getPinsFriendsMap(List<Long> uids) {
        List<Post> posts = new ArrayList<>();
        for (Long uid : uids) {
            posts.addAll(postRepository.findNonePrivateByUid(uid));
        }

        return posts.stream().map(PostDto::pinMap).collect(Collectors.toList());
    }

    //친구 post list preview
    public List<PostDto> getPostsFriendsList(List<Long> uids) {
        List<Post> posts = new ArrayList<>();
        for (Long uid : uids) {
            posts.addAll(postRepository.findNonePrivateByUid(uid));
        }
        return getPostPreviewListDtos(posts);
    }

    //Public
    public List<PostDto> getPinsPublicMap(float longitude, float latitude) {
        List<Post> posts = postRepository.findPublicPosts(longitude, latitude);
        return posts.stream().map(PostDto::pinMap).collect(Collectors.toList());
    }

    public List<PostDto> getPostsPublicList(float longitude, float latitude) {
        List<Post> posts = postRepository.findPublicPosts(longitude, latitude);
        return getPostPreviewListDtos(posts);
    }

    private List<PostDto> getPostPreviewListDtos(List<Post> posts) {
        List<PostDto> postDtos = new ArrayList<>();
        for (Post post : posts) {
            List<PostImage> postImageEntities = postImageDataRepository.findByPostId(post.getId());
            List<String> postImageNames = new ArrayList<>();
            for (PostImage postImage : postImageEntities) {
                postImageNames.add(postImage.getPostImageName());
            }
            PostDto postDto = PostDto.postPreviewList(post, likeRepository, postImageNames);
            postDtos.add(postDto);
        }
        return postDtos;
    }

    private List<PostDto> getPostPreviewMapDtos(List<Post> posts) {
        List<PostDto> postDtos = new ArrayList<>();
        for (Post post : posts) {
            List<PostImage> postImageEntities = postImageDataRepository.findByPostId(post.getId());
            List<String> postImageNames = new ArrayList<>();
            for (PostImage postImage : postImageEntities) {
                postImageNames.add(postImage.getPostImageName());
            }
            PostDto postDto = PostDto.postPreviewMap(post, postImageNames);
            postDtos.add(postDto);
        }
        return postDtos;
    }
}
