package com.b6122.ping.service;

import com.b6122.ping.domain.Friendship;
import com.b6122.ping.domain.User;
import com.b6122.ping.domain.UserRole;
import com.b6122.ping.repository.datajpa.FriendshipDataRepository;
import com.b6122.ping.repository.datajpa.PostDataRepository;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class FriendshipServiceTest {

    @Autowired
    UserDataRepository userDataRepository;

    @Autowired
    PostDataRepository postDataRepository;

    @Autowired
    FriendshipService friendshipService;

    @Autowired
    FriendshipDataRepository friendshipDataRepository;

    @Test
    @DisplayName("친구추가")
    @Transactional
    void 친구추가() {
        // given
        User user1 = User.builder()
                .username("user1")
                .nickname("user1Nickname")

                .role(UserRole.ROLE_USER)
                .build();
        User user2 = User.builder()
                .username("user2")
                .nickname("user2Nickname")
                .role(UserRole.ROLE_USER)
                .build();

        userDataRepository.save(user1);
        userDataRepository.save(user2);

        //when
        Friendship friendship = Friendship.createFriendship(user1, user2);
        friendshipDataRepository.save(friendship);

        Assertions.assertEquals(user1.getFromUserFriendships().size(),1);
        Assertions.assertEquals(user1.getToUserFriendships().size(),0);
        Assertions.assertEquals(user2.getToUserFriendships().size(),1);
        Assertions.assertEquals(user2.getFromUserFriendships().size(),0);
    }

    @Test
    @DisplayName("친구삭제시캐스케이드테스트")
    @Transactional
    void 친구삭제시캐스케이드테스트() {
        // given
        User user1 = User.builder()
                .username("user1")
                .nickname("user1Nickname")

                .role(UserRole.ROLE_USER)
                .build();
        User user2 = User.builder()
                .username("user2")
                .nickname("user2Nickname")
                .role(UserRole.ROLE_USER)
                .build();

        userDataRepository.save(user1);
        userDataRepository.save(user2);

        Friendship friendship = Friendship.createFriendship(user1, user2);
        friendshipDataRepository.save(friendship);

        //when
        userDataRepository.delete(user1);

        //then
        Assertions.assertEquals(friendshipDataRepository.findFriendshipsById(friendship.getId()).size(),0);
    }
}