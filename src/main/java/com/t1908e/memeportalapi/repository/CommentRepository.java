package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment,Integer> {
    List<Comment> findCommentsByPostIdAndStatus(int postId,int status);
}
