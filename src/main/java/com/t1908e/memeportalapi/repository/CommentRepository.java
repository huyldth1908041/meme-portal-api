package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    Page<Comment> findAllByRepliedCommentIdAndPostId(int replyCommentId, int postId, Pageable pageable);

    @Query("SELECT COUNT(comment) FROM Comment comment WHERE comment.repliedCommentId = :commentId")
    int countRepliedComment(@Param(value = "commentId") int commentId);

    Page<Comment> findAllByRepliedCommentId(int commentId, Pageable pageable);
}
