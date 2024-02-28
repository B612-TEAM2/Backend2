package com.b6122.ping.controller;

import com.b6122.ping.auth.PrincipalDetails;
import com.b6122.ping.dto.*;
import com.b6122.ping.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class RestApiController {

    private final JwtService jwtService;
    private final UserService userService;
    private final FriendshipService friendshipService;
    private final OauthService oauthService;

    //프론트엔드로부터 authorization code 받고 -> 그 code로 카카오에 accesstoken 요청
    // 받아 온 access token으로 카카오 리소스 서버로부터 카카오 유저 정보 가져오기
    // 가져온 정보를 기반으로 회원가입
    // jwt accessToken을 리액트 서버에 return
//    @PostMapping("/oauth/jwt")
//    public ResponseEntity<Map<String, Object>> oauthLogin(String server, String code) throws IOException {
//        UserDto joinedUser = oauthService.join(server, code);
//        return ResponseEntity.ok().body(jwtService.createJwtAccessAndRefreshToken(joinedUser));
//    }
//
//    @GetMapping("/auth/{serverName}/callback")
//    public void getCode(@PathVariable("serverName") String server,
//                          @RequestParam("code") String code) throws IOException {
//            oauthLogin(server, code);
//    }

    @GetMapping("/jwt/access")
    public void jwt() {

    }

    @PostMapping("/oauth/jwt/{serverName}")
    public ResponseEntity<Map<String, Object>> oauthLogin(@PathVariable("serverName") String server,
                                                          @RequestBody Map<String, Object> request) throws IOException {

        UserDto joinedUser = oauthService.join(server, request.get("code").toString());
        return ResponseEntity.ok().body(jwtService.createJwtAccessAndRefreshToken(joinedUser));
    }

    @PostMapping("/profile")
    public void setInitialProfile(@RequestParam(value = "profileImg", required = false) MultipartFile profileImg,
                                  @RequestParam("nickname") String nickname,
                                  Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUser().getId();
        UserProfileReqDto reqDto = new UserProfileReqDto(nickname, profileImg, userId);
        userService.updateProfile(reqDto);
    }

    //회원 탈퇴
    @DeleteMapping("/account")
    public void deleteAccount(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        userService.deleteAccount(principalDetails.getUser().getId());
    }

    //사용자 정보(닉네임, 사진) 가져오기
    @GetMapping("/account")
    public ResponseEntity<UserProfileResDto> account(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        UserProfileResDto resDto = userService.getUserProfile(principalDetails.getUser().getId());
        return ResponseEntity.ok().body(resDto);
    }

    //회원정보 변경(일단 사진만, 닉네임까지 확장 가능)
    @PostMapping("/account")
    public void updateProfile(@RequestParam(value = "profileImg", required = false) MultipartFile profileImg,
                                   @RequestParam("nickname") String nickname,
                                   Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUser().getId();
        UserProfileReqDto reqDto = new UserProfileReqDto(nickname, profileImg, userId);
        userService.updateProfile(reqDto);
    }


    /**
     * 친구 목록 불러오기 (자기 친구)
     */
    @GetMapping("/friends")
    public ResponseEntity<List<UserProfileResDto>> getFriendsList(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        List<UserProfileResDto> result = friendshipService.getFriendsProfile(principalDetails.getUser().getId());
        return ResponseEntity.ok().body(result);
    }

    /**
     * 친구삭제
     * @param request {"nickname" : "xxx"}
     */
    @DeleteMapping("/friends")
    public void deleteFriend(@RequestBody Map<String, Object> request, Authentication authentication) {
        String friendNickname = request.get("nickname").toString();
        Long friendId = userService.findUserByNickname(friendNickname);

        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUser().getId();

        friendshipService.deleteFriend(userId, friendId);

    }

    /**
     * 사용자의 nickname을 검색하여 찾기
     * @param nickname
     * @return 사용자 정보(UserProfileResDto -> nickname, profileImg, id), 친구 여부
     */
    @GetMapping("/friends/search")
    public ResponseEntity<SearchUserResDto> searchUser(@RequestParam("nickname") String nickname,
                                                       Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long friendId = userService.findUserByNickname(nickname);
        SearchUserResDto searchUserResDto = friendshipService
                .searchUser(friendId, principalDetails.getUser().getId());
        return ResponseEntity.ok().body(searchUserResDto);
    }

    /**
     * 친구 신청하기
     */
    @PostMapping("/friends/search")
    public void sendFriendRequest(Authentication authentication,
                                  @RequestBody SendFriendReqDto reqDto) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long fromUserId = principalDetails.getUser().getId();
        Long toUserId = userService.findUserByNickname(reqDto.getNickname());
        //fromUserId -> 친구 신청한 사람 id, toUserId -> 친구 신청 상대방 id
        friendshipService.sendRequest(fromUserId, toUserId);
    }

    /**
     * 나에게 온 친구 요청(대기중 PENDING) 리스트
     * @return
     */
    @GetMapping("/friends/pending")
    public ResponseEntity<List<UserProfileResDto>> friendsRequestList(Authentication authentication) {
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        List<UserProfileResDto> result = friendshipService.findPendingFriendsToMe(principalDetails.getUser().getId());

        return ResponseEntity.ok().body(result);
    }

    /**
     * 친구 요청 수락 또는 거절
     * nickname , status(accept, reject)
     */
    @PostMapping("/friends/pending")
    public void addFriend(Authentication authentication,
                          @RequestBody AddFriendReqDto reqDto) {
        //toUserId -> 친구 요청을 받은 유저
        //fromUserId -> 친구 요청을 보낸 유저
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        Long userId = principalDetails.getUser().getId();
        reqDto.setToUserId(userId);
        friendshipService.addFriend(reqDto);
    }
}
