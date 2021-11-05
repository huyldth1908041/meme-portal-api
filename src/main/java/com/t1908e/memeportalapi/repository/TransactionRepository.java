package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.Transaction;
import com.t1908e.memeportalapi.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<Transaction, Integer>, JpaSpecificationExecutor<Transaction> {
    Page<Transaction> findAll(Specification<Transaction> spec, Pageable pageable);

    int countAllByStatus(int status);

    @Query(nativeQuery = true, value = "SELECT sum(amount) AS total_amount FROM transaction WHERE status = 2")
    double getTotalTokenSpent();
}
