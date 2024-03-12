package com.b6122.ping.controller;

import com.b6122.ping.auth.PrincipalDetails;
import com.b6122.ping.domain.PostScope;
import com.b6122.ping.dto.*;
import com.b6122.ping.dto.PostPreviewListDto;
import com.b6122.ping.dto.PostPreviewMapDto;
import com.b6122.ping.service.PostService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                                        @RequestParam(value = "imgs", required = false) List<MultipartFile> imgs,
                                        Authentication authentication){
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

        PostDto postDto = new PostDto();
        postDto.setTitle(title);
        postDto.setContent(content);
        postDto.setLatitude(latitude);
        postDto.setLongitude(longitude);
        postDto.setUid(principalDetails.getUser().getId());
        if("private".equals(scope)){
            postDto.setScope(PostScope.PRIVATE);
        } else if("public".equals(scope)) {
            postDto.setScope(PostScope.PUBLIC);
        } else {
            postDto.setScope(PostScope.FRIENDS);
        }
        Long pid = postService.createPost(postDto, imgs);
        return ResponseEntity.status(HttpStatus.CREATED).body(pid);
    }

    //글 수정 후 디비 저장
    @PutMapping("/posts/home/edit/{postId}")
    public ResponseEntity modifyPost(@RequestParam("title") String title,
                                     @RequestParam("content") String content,
                                     @RequestParam("latitude") float latitude,
                                     @RequestParam("longitude") float longitude,
                                     @RequestParam("scope") String scope,
                                     @RequestParam(value = "imgs", required = false) List<MultipartFile> imgs,
                                     @PathVariable("postId") Long requestPid,
                                     Authentication authentication) throws IOException {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        PostDto postDto = new PostDto();
        postDto.setId(requestPid);
        postDto.setTitle(title);
        postDto.setContent(content);
        postDto.setLatitude(latitude);
        postDto.setLongitude(longitude);
        postDto.setUid(principalDetails.getUser().getId());
        if("private".equals(scope)){
            postDto.setScope(PostScope.PRIVATE);
        } else if("public".equals(scope)) {
            postDto.setScope(PostScope.PUBLIC);
        } else {
            postDto.setScope(PostScope.FRIENDS);
        }
        Long pid = postService.modifyPost(postDto, imgs);
        return ResponseEntity.status(HttpStatus.CREATED).body(pid);
    }

    //글 삭제
    @DeleteMapping("/post/delete")
        public ResponseEntity deletepost(@RequestBody Map<String, Object> request) throws IOException {
        Integer pid = (Integer) request.get("pid");
        postService.deletePost(pid.longValue());
        return ResponseEntity.ok(pid);
    }

    @PostMapping("/likeToggle")
    public ResponseEntity<Map<String, Object>> clickLike(@RequestParam("pid") String postId,
                                    @RequestParam("isLike") Boolean isLike,
                                    Authentication authentication){
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        Long pid = Long.valueOf(postId);
        int updatedLikeCount = postService.toggleLike(pid, uid, isLike);
        Map<String, Object> map = new HashMap<>();
        map.put("pid", pid);
        map.put("isLike", isLike);
        map.put("likeCount", updatedLikeCount);
        return ResponseEntity.ok().body(map);
    }


     /*
     // 좋아요 토글 한 번에 여러개 처리
    public ResponseEntity<String> toggleLike(@RequestParam List<Long> pids, @RequestParam List<Boolean> myLikes,Authentication authentication ) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        postService.toggleLike(pids, myLikes, uid);

        return ResponseEntity.ok("Like toggled successfully");
    }
    */


    //글 정보 반환, 조회수 ++
    @GetMapping("/postInfo")
    public ResponseEntity<PostInfoDto> postInfo(@RequestParam("pid") Long pid,Authentication authentication ) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        PostInfoDto pd = postService.getPostInfo(pid, uid);
        System.out.println("PostDto = " + pd);
        return ResponseEntity.ok(pd);
    }


    //pin클릭 시 글 목록 반환, pid 리스트를 받아 반환, home friends public 동일
    @GetMapping("/posts/clickPin")//map -> clickPin 변경
    public ResponseEntity<List<PostPreviewMapDto>> postsPreviewPin(@RequestParam List<Long> pids){
        List<PostPreviewMapDto> postsPreviewMap = postService.getPostsPreviewPin(pids);
        return ResponseEntity.ok(postsPreviewMap);
    }

    //Home

    //Home-Map, 내 모든 글의 pin 반환
    @GetMapping("/posts/home/pins")
    public ResponseEntity<List<PinDto>> showPinsHome(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        List<PinDto> pinsDto = postService.getPinsHomeMap(uid);
        return ResponseEntity.ok(pinsDto);
    }

    //Home-List 토글, postList 반환
    @GetMapping("/posts/home/list")
    public ResponseEntity<List<PostPreviewListDto>> showPostsHomeList(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long uid = principalDetails.getUser().getId();
        List<PostPreviewListDto> posts = postService.getPostsHomeList(uid);
        return ResponseEntity.ok().body(posts);
    }


    //Friend

    //pins 반환, 친구 id 를 리스트로 받아 공개범위가 private이 아닌 것만 pid, 위도 경도 반환
    @GetMapping("/posts/friends/pins")
    public ResponseEntity<List<PinDto>> showPinsFriends(@RequestParam("uids") List<Long> uids) {
        List<PinDto> posts = postService.getPinsFriendsMap(uids);
        return ResponseEntity.ok(posts);
    }

    //친구 글 목록 preview 반환, 친구 id를 리스트로 받아 scope가 friend, public 인 것만 최신순으로 반환
    @GetMapping("/posts/friends/list")
    public ResponseEntity<List<PostPreviewListDto>> showPostsFriendsList(@RequestParam("uids") List<Long> uids) {
        List<PostPreviewListDto> posts = postService.getPostsFriendsList(uids);
        return ResponseEntity.ok().body(posts);
    }




    //public

    //public pin반환, 반경 2km 내에 있는 글 반환
    @GetMapping("/posts/public/pins")
    public ResponseEntity<List<PinDto>> showPinsPubic(@RequestParam("longitude") float longitude, @RequestParam("latitude") float latitude) {
        List<PinDto> pins = postService.getPinsPublicMap(longitude,latitude);
        return ResponseEntity.ok(pins);
    }


    //public list 반환,반경 2km 내에 있는 글 반환

    @GetMapping("/posts/public/list")
    public ResponseEntity<List<PostPreviewListDto>> showPostsPubicList(@RequestParam("longitude") float longitude, @RequestParam("latitude") float latitude) {
        List<PostPreviewListDto> posts = postService.getPostsPublicList(longitude,latitude);
        return ResponseEntity.ok().body(posts);
    }


}

