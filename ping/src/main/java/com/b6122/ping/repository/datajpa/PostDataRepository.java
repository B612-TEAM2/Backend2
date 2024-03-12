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

    @Query(value = "SELECT * FROM post p WHERE p.scope = 'PUBLIC' AND ST_Distance_Sphere(POINT(p.longitude, p.latitude), POINT(:longitude, :latitude)) <= 50000 ORDER BY p.created_date", nativeQuery = true)
    List<Post> findPublicPosts(@Param("longitude") float longitude, @Param("latitude") float latitude);

//likeCount update
}