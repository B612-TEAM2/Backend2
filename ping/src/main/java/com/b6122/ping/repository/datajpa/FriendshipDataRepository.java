package com.b6122.ping.repository.datajpa;

import com.b6122.ping.domain.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipDataRepository extends JpaRepository<Friendship, Long> {

    //친구 목록 조회
    @Query("select f from Friendship f" +
            " join fetch f.fromUser fu" +
            " join fetch f.toUser tu" +
            " where (fu.id = :userId or tu.id = :userId) and f.isFriend = true")
    List<Friendship> findFriendshipsById(@Param("userId") Long userId);

    //친구 단건 조회
    @Query("select f from Friendship f" +
            " where f.isFriend = true" +
            " and ((f.toUser.id =:friendId and f.fromUser.id = :userId)" +
            " or (f.toUser.id = :userId and f.fromUser.id = :friendId))")
    Optional<Friendship> findFriendshipByIds(@Param("friendId") Long friendId,
                                        @Param("userId") Long userId);

    /**
     * 아직 대기 중인(PENDING) 친구 요청
     * @param toUserId 친구 요청 받은 사람 id
     * @param fromUserId 친구 요청 보낸 사람 id
     * @return
     */
    @Query("select f from Friendship f" +
            " where f.isFriend = false" +
            " and f.requestStatus = com.b6122.ping.domain.FriendshipRequestStatus.PENDING" +
            " and f.toUser.id = :toUserId" +
            " and f.fromUser.id = :fromUserId")
    Optional<Friendship> findPendingFriendShip(@Param("toUserId") Long toUserId,
            @Param("fromUserId") Long fromUserId);

    /**
     * 나에게 온 대기 상태의 친구 요청 찾기 (toUserId가 내 id)
     * @param toUserId
     * @return
     */
    @Query("select f from Friendship f" +
            " join fetch f.fromUser" +
            " where f.isFriend = false" +
            " and f.requestStatus = com.b6122.ping.domain.FriendshipRequestStatus.PENDING" +
            " and f.toUser.id = :toUserId")
    List<Friendship> findPendingFriendShipsToMe(@Param("toUserId") Long toUserId);
}

