package com.b6122.ping.service;

import com.b6122.ping.domain.Post;
import com.b6122.ping.domain.User;
import com.b6122.ping.domain.UserRole;
import com.b6122.ping.repository.datajpa.PostDataRepository;
import com.b6122.ping.repository.datajpa.UserDataRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FriendshipServiceTest {

    @Autowired
    UserDataRepository userDataRepository;

    @Autowired
    PostDataRepository postDataRepository;

    @Test
    @DisplayName("친구추가")
    @Transactional
    void 친구추가() {
        User user1 = User.builder()
                .username("user1")
                .nickname("user1Nickname")
                .role(UserRole.ROLE_USER)
                .build();

        Long userId = userDataRepository.save(user1).getId();

        Post post1 = new Post();
        postDataRepository.save(post1);

        post1.setUser(user1);
        Assertions.assertEquals(user1.getPosts().size(),1);
        Post post2 = new Post();
        postDataRepository.save(post2);

        post2.setUser(user1);

        postDataRepository.save(post1);
        postDataRepository.save(post2);

        Optional<User> findUser = userDataRepository.findById(userId);
        Assertions.assertEquals(findUser.get().getPosts().size(), 2);

        userDataRepository.delete(findUser.get());
        Assertions.assertEquals(postDataRepository.findAll().size(), 0);

    }
}