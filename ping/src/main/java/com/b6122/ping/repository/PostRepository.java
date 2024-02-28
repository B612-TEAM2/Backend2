package com.b6122.ping.repository;

import com.b6122.ping.domain.Post;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;

import java.lang.management.RuntimeMXBean;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    private final EntityManager em;

    @Query("SELECT p FROM Post p WHERE p.latitude = :latitude AND p.longitude = :longitude ORDER BY p.createdDate DESC")
    public List<Post> findByLocation(@Param("latitude") float latitude, @Param("longitude") float longitude) {
        return null;
    }
    @Query("SELECT p FROM Post p WHERE p.latitude = :latitude AND p.longitude = :longitude AND p.user.id = :uid ORDER BY p.createdDate DESC")
    public List<Post> findByLocationUser(@Param("latitude") float latitude, @Param("longitude") float longitude, @Param("uid") long uid) {
        return null;
    }

    @Query("Delete FROM Post p WHERE p.pid = :pid")
    public List<Post> deletePost(@Param("pid") Long pid) {
        return null;
    }

    @Modifying// 조회수 중복 방지 추가 구현
    @Query("update Post p set p.viewCount = :viewCount where p.id =:id")
    public int updateViewCount(@Param("viewCount") int viewCount, @Param("id") Long id){
        return 0;
    }


    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id =:pid")
    public int upLikeCount(@Param("id") Long pid){
        return 0;
    }

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount - 1 where p.id =:pid")
    public int downLikeCount(@Param("id") Long pid){
        return 0;
    }


    public  Post findById(Long id){
        return em.createQuery("select p from Post p where p.id = :id", Post.class)
                .setParameter("id", id)
                .getSingleResult();
    }

//    @Query("SELECT p FROM Post p WHERE p.user.id= :uid  ORDER BY p.createdDate DESC")
    public List<Post> findByUid(Long uid){
        return em.createQuery("select p from Post p" +
                        " where p.user.id = :uid" +
                        " order by p.createdDate", Post.class) //최신순으로 반환
                .setParameter("uid", uid)
                .getResultList();
    }

    public Long save(Post p) {
        em.persist(p);
        return p.getId();
    }

    public long updatePost(Post p) {
        return em.createQuery("update Post p set location =:location, latitude =:latitude, longitude =:longitude, title =:title, content =:content, scope =:scope, imgPaths =:imgPaths", Long.class)
                .setParameter("location", p.getLocation())
                .setParameter("latitude", p.getLatitude())
                .setParameter("longitude", p.getLongitude())
                .setParameter("title", p.getTitle())
                .setParameter("content", p.getContent())
                .setParameter("scope", p.getScope())
                .setParameter("imgPaths", p.getImgPaths())
                .executeUpdate();
    }

    public List<Post> findNonePrivateByUid(Long uid) {
        return em.createQuery("select p from Post p" +

                " where p.user.id = :uid and p.scope != \"private\" " +
                        "order by p.createdDate", Post.class) //최신순
                .setParameter("uid", uid)
                .getResultList();
    }

    public List<Post> findPublicPosts(float longitude, float latitude) {
        return em.createQuery("select p from Post p"+
                " where p.scope = \"public\" and ST_Distance_Sphere(POINT(p.longitude, p.latitude), POINT(longitude, latitude)) <= 2000"+
                "order by p.createdDate", Post.class)
                .getResultList();
    }
}
