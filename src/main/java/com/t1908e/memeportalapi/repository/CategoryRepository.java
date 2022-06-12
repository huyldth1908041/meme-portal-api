package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findCategoryByStatus(int status);
    List<Category> findAllByName(String name);
}
