package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, Long> , JpaSpecificationExecutor<User> {
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    List<User> findAllByStatus(int status);

    @Query("SELECT user FROM User user WHERE user.createdAt <= :createdAt AND user.status = :status")
    ArrayList<User> findAllWithCreateAtBefore(
            @Param(value ="createdAt") Date createdAt, @Param(value ="status") int status );
}
