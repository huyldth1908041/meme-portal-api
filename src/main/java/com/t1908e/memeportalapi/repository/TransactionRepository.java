package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Integer> {
}
