package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Integer> {
    Optional<PostLike> findPostLikeByPostIdAndUserId(int postId,int userId);
}
