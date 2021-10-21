package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Integer> {
    Optional<CommentLike> findCommentLikeByCommentIdAndUserId(int commentId, long userId);
}
