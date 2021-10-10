package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
