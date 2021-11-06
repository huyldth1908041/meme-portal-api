package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.PostShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostShareRepository extends JpaRepository<PostShare, Integer> {
    List<PostShare> findAllByPostIdAndUserIdOrderByCreatedAtDesc(int postId, long userId);

    int countByPostId(int postId);
}
