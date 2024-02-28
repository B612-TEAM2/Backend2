package com.b6122.ping.controller;

import com.b6122.ping.auth.PrincipalDetails;
import com.b6122.ping.domain.PostScope;
import com.b6122.ping.dto.PostDto;
import com.b6122.ping.service.PostService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.apache.catalina.filters.AddDefaultCharsetFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter@Setter
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PostController {
    private final PostService postService;

    //글 작성 후 디비 저장
    @PostMapping("/posts/home/store")
    public ResponseEntity<Long> getPost(@RequestParam("title") String title,
                                        @RequestParam("content") String content,
                                        @RequestParam("latitude") float latitude,
                                        @RequestParam("longitude") float longitude,
                                        @RequestParam("scope") String scope,
                                        @RequestParam(value = "img", required = false) List<MultipartFile> img,
                                        Authentication authentication){
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        PostDto postDto = new PostDto();
        postDto.setTitle(title);
        postDto.setContent(content);
        postDto.setLatitude(latitude);
        postDto.setLongitude(longitude);
        postDto.setImgs(img);
        postDto.setUid(principalDetails.getUser().getId());
        if("private".equals(scope)){
            postDto.setScope(PostScope.PRIVATE);
        } else if("public".equals(scope)) {
            postDto.setScope(PostScope.PUBLIC);
        } else {
            postDto.setScope(PostScope.FRIENDS);
        }
        Long pid = postService.createPost(postDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(pid);
    }

    //글 수정 후 디비 저장
    @PutMapping("/posts/home/edit/{postId}")
    public ResponseEntity modifyPost(@RequestParam("title") String title,
                                     @RequestParam("content") String content,
                                     @RequestParam("latitude") float latitude,
                                     @RequestParam("longitude") float longitude,
                                     @RequestParam("scope") String scope,
                                     @RequestParam(value = "img", required = false) List<MultipartFile> img,
                                     @PathVariable("postId") Long postId,
                                     Authentication authentication){

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        PostDto postDto = new PostDto();
        postDto.setTitle(title);
        postDto.setContent(content);
        postDto.setLatitude(latitude);
        postDto.setLongitude(longitude);
        postDto.setImgs(img);
        postDto.setUid(principalDetails.getUser().getId());
        postDto.setId(postId);
        if("private".equals(scope)){
            postDto.setScope(PostScope.PRIVATE);
        } else if("public".equals(scope)) {
            postDto.setScope(PostScope.PUBLIC);
        } else {
            postDto.setScope(PostScope.FRIENDS);
        }
        Long pid = postService.modifyPost(postDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(pid);
    }

    //글 삭제
    @PostMapping("/post/delete")
        public ResponseEntity deletepost(@RequestParam("pid") Long pid){
        postService.deletePost(pid);
        return ResponseEntity.ok(pid);
    }

    //글 정보 반환, 조회수 ++
    @GetMapping("/postInfo")
    public ResponseEntity<PostDto> postInfo(@RequestParam("pid") Long pid,Authentication authentication ) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        PostDto pd = postService.getPostInfo(pid, uid);
        System.out.println("PostDto = " + pd);
        return ResponseEntity.ok(pd);
    }

    //좋아요 update
    @PostMapping("/likeToggle")

    public ResponseEntity<String> toggleLike(@RequestParam List<Long> pids, @RequestParam List<Boolean> myLikes,Authentication authentication ) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        postService.toggleLike(pids, myLikes, uid);

        return ResponseEntity.ok("Like toggled successfully");
    }


    //pin클릭 시 글 목록 반환, pid 리스트를 받아 반환, home friends public 동일
    @GetMapping("/posts/clickPin")//map -> clickPin 변경
    public ResponseEntity<List<PostDto>> postsPreviewPin(@RequestParam List<Long> pids){
        List<PostDto> posts = postService.getPostsPreviewPin(pids);
        return ResponseEntity.ok(posts);
    }

    //Home

    //Home-Map, 내 모든 글의 pin 반환
    @GetMapping("/posts/home/pins")
    public ResponseEntity<List<PostDto>> showPinsHome(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        List<PostDto> posts = postService.getPinsHomeMap(uid);
        return ResponseEntity.ok(posts);
    }

    //Home-List 토글, postList 반환
    @GetMapping("/posts/home/list")
    public ResponseEntity<List<PostDto>> showPostsHomeList(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        List<PostDto> posts = postService.getPostsHomeList(uid);
        return ResponseEntity.ok().body(posts);
    }




    //Friend

    //pins 반환, 친구 id 를 리스트로 받아 공개범위가 private이 아닌 것만 pid, 위도 경도 반환
    @GetMapping("/posts/friends/pins")
    public ResponseEntity<List<PostDto>> showPinsFriends(@RequestParam("uids") List<Long> uids) {
        List<PostDto> posts = postService.getPinsFriendsMap(uids);
        return ResponseEntity.ok(posts);
    }


    //친구 글 목록 preview 반환, 친구 id를 리스트로 받아 scope가 friend, public 인 것만 최신순으로 반환
    @GetMapping("/posts/friends/list")
    public ResponseEntity<List<PostDto>> showPostsFriendsList(@RequestParam("uids") List<Long> uids) {
        List<PostDto> posts = postService.getPostsFriendsList(uids);
        return ResponseEntity.ok().body(posts);
    }




    //public

    //public pin반환, 반경 2km 내에 있는 글 반환
    @GetMapping("/posts/public/pins")
    public ResponseEntity<List<PostDto>> showPinsPubic(@RequestParam("longitude") float longitude, @RequestParam("latitude") float latitude) {
        List<PostDto> posts = postService.getPinsPublicMap(longitude,latitude);
        return ResponseEntity.ok(posts);
    }


    //public list 반환,반경 2km 내에 있는 글 반환

    @GetMapping("/posts/public/list")
    public ResponseEntity<List<PostDto>> showPostsPubicList(@RequestParam("longitude") float longitude, @RequestParam("latitude") float latitude) {
        List<PostDto> posts = postService.getPostsPublicList(longitude,latitude);
        return ResponseEntity.ok(posts);
    }
}

