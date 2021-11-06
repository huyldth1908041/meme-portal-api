package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.PushHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PushHistoryRepository extends JpaRepository<PushHistory, Integer> {
    List<PushHistory> findAllByUserIdAndPostIdAndStatusGreaterThan(long userId, int postId, int status);

    Page<PushHistory> findAllByPostIdAndStatusGreaterThan(int postId, int status, Pageable pageable);

    int countByPostIdAndStatusGreaterThan(int postId, int status);

}
