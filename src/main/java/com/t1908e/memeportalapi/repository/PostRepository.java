package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Integer> {
}
