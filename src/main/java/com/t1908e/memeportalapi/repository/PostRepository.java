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
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer>, JpaSpecificationExecutor<Post> {
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query("update Post post set post.status = :status where post.id in :ids")
    int changePostStatus(@Param(value = "ids") ArrayList<Integer> ids, @Param(value = "status") int status);

    @Modifying(flushAutomatically = true)
    @Query("update Post  post set post.status = :status where post.userId = :userId")
    int changePostStatusAccordingUserId(@Param(value = "userId") long userId, @Param(value = "status") int status);

    @Query("SELECT post FROM Post post WHERE post.userId = :userId")
    List<Post> findAllByUserId(@Param(value = "userId") long userId);

    int countAllByStatusGreaterThan(int status);
    int countAllByStatus(int status);
    @Query(nativeQuery = true, value = "SELECT post.created_at, COUNT(*) AS post_count FROM `post` WHERE status > 0 AND created_at >= NOW() - INTERVAL :days DAY and created_at <= NOW() GROUP BY DATE(created_at) ORDER BY created_at ASC")
    List<Object[]> countPostByCreatedDate(@Param(value="days") int days);

}

