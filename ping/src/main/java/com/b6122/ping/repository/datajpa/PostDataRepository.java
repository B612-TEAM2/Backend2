package com.b6122.ping.repository.datajpa;
import com.b6122.ping.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

//CRUD작성
public interface PostDataRepository extends JpaRepository<Post, Long> {
    @Modifying
    @Query("update Post p set p.viewCount = :viewCount where p.id =:id")
    int updateViewCount(@Param("viewCount") int viewCount, @Param("id") Long id);
//likeCount update
}