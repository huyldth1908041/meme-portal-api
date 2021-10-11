package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Post;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer>, JpaSpecificationExecutor<Post> {
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("update Post post set post.status = :status where post.id in :ids")
    int changePostStatus(@Param(value = "ids") ArrayList<Integer> ids, @Param(value = "status") int status);


    Optional<Post> findById(int id);
}

