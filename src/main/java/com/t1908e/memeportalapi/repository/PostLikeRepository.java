package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    Optional<PostLike> findPostLikeByPostIdAndUserId(int postId, long userId);

    int countByPostId(int postId);

    @Query(value = "SELECT pl.userId FROM PostLike pl WHERE pl.postId = :postId")
    List<Long> findListUserIdLiked(@Param(value = "postId") int postId);

}
