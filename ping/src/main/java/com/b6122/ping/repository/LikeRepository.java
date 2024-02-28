package com.b6122.ping.repository;

import com.b6122.ping.domain.Like;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class LikeRepository {

    private final EntityManager em;


    //특정post에 좋아요를 눌렀는지 확인
    public boolean checkMyLike(Long pid, Long uid) {
        TypedQuery<Like> query = em.createQuery("SELECT l FROM Like l WHERE l.post.id = :pid AND l.user.id = :uid", Like.class);
        query.setParameter("pid", pid);
        query.setParameter("uid", uid);

        List<Like> result = query.getResultList();

        return !result.isEmpty(); // If the result list is not empty, return true; otherwise, return false
    }

    /*
    public Optional<Like> findByPostIdAndUserId(@Param("pid") long pid, @Param("uid") long uid) {
        TypedQuery<Like> query = em.createQuery("SELECT l FROM Like l WHERE l.pid = :pid AND l.uid = :uid", Like.class);
        query.setParameter("pid", pid);
        query.setParameter("uid", uid);
        Optional<Like> like = Optional.ofNullable(query.getSingleResult());
        return like ;
    }
*/

    public void save(@Param("pids") List<Long> pids,  @Param("uid") Long uid ) {
        for(Long pid : pids){
            TypedQuery<Like> query = em.createQuery("Insert INTO Like l(pid, uid) VALUES (:pid, :uid)", Like.class);
            query.setParameter("pid", pid);
            query.setParameter("uid", uid);
        }

    }

    public void delete(@Param("pids") List<Long> pids, @Param("uid") Long uid ){
        TypedQuery<Like> query = em.createQuery("delete FROM Like l WHERE l.pid in :pids And l.uid = :uid", Like.class);
        query.setParameter("pids", pids);
        query.setParameter("uid", uid);
    }


}
