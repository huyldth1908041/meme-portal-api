package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.dto.TopCreatorDTO;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.HashMap;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> , JpaSpecificationExecutor<User> {
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    List<User> findAllByStatus(int status);

    List<User> findByStatusGreaterThanAndTokenBalanceGreaterThanOrderByTokenBalanceDesc(int status, double token, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT user.*, COUNT(post.id) AS post_count " +
            "FROM user INNER JOIN post ON user.id = post.user_id " +
            "WHERE user.status > 0 AND post.status > 0 " +
            "GROUP BY user.id ORDER BY post_count DESC LIMIT 5")
    List<User> findTopCreator();
}
