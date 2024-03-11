package com.b6122.ping.domain;

import com.b6122.ping.dto.AddFriendReqDto;
import com.b6122.ping.dto.UserProfileResDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id")
    private User fromUser; //친구 요청을 보낸 User

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id")
    private User toUser; //친구 요청을 받은 User

    @Enumerated(EnumType.STRING)
    private FriendshipRequestStatus requestStatus; // PENDING, ACCEPTED, REJECTED

    private boolean isFriend = false;

    //친구 요청 시 메소드
    public static Friendship createFriendship(User fromUser, User toUser) {
        Friendship friendship = new Friendship();
        friendship.fromUser = fromUser;
        friendship.toUser = toUser;
        friendship.requestStatus = FriendshipRequestStatus.PENDING;
        fromUser.addFromUserFriendships(friendship);
        toUser.addToUserFriendships(friendship);
        return friendship;
    }

    //친구 상대방 유저 정보
    public UserProfileResDto getUserProfileOfFriendship(Long userId) {
        User fromUser = this.getFromUser();
        User toUser = this.getToUser();
        UserProfileResDto resDto;

        //사용자가 친구 요청을 했을 경우 친구 상대방은 toUser
        if (fromUser.getId().equals(userId)) {
            resDto = toUser.getProfileInfo();

        //사용자가 친구 요청을 받았을 경우 친구 상대방은 fromUser
        } else {
            resDto = fromUser.getProfileInfo();
        }
        return resDto;
    }

    /**
     * 친구 거절 또는 수락 시
     * @param reqDto String nickname, String status, Long toUserId;
     */
    public void updateFriendship(AddFriendReqDto reqDto) {
        List<Friendship> fromUserFriendships= this.getFromUser().getFromUserFriendships();
        List<Friendship> toUserFriendships = this.getToUser().getToUserFriendships();
        if ("accept".equals(reqDto.getStatus())) {
            this.requestStatus = FriendshipRequestStatus.ACCEPTED;
            this.isFriend = true;
            for (Friendship fromUserFriendship : fromUserFriendships) {
                if(fromUserFriendship.equals(this)) {
                    fromUserFriendship.isFriend = true;
                    fromUserFriendship.requestStatus = FriendshipRequestStatus.ACCEPTED;
                }
            }
            for (Friendship toUserFriendship : toUserFriendships) {
                if(toUserFriendship.equals(this)) {
                    toUserFriendship.isFriend = true;
                    toUserFriendship.requestStatus = FriendshipRequestStatus.ACCEPTED;
                }
            }
        } else {
            this.requestStatus = FriendshipRequestStatus.REJECTED;
            for (Friendship fromUserFriendship : fromUserFriendships) {
                if(fromUserFriendship.equals(this)) {
                    fromUserFriendship.requestStatus = FriendshipRequestStatus.REJECTED;
                }
            }
            for (Friendship toUserFriendship : toUserFriendships) {
                if(toUserFriendship.equals(this)) {
                    toUserFriendship.requestStatus = FriendshipRequestStatus.REJECTED;
                }
            }
        }

    }
}

