package com.b6122.ping.repository.datajpa;

import com.b6122.ping.domain.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageDataRepository extends JpaRepository<PostImage, Long> {

    @Query("select pi from PostImage pi where pi.post.id = :postId")
    List<PostImage> findByPostId(@Param("postId") Long postId);
}
