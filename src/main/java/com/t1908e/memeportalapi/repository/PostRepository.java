package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Set;


public interface PostRepository extends PagingAndSortingRepository<Post,Integer> {
//    @Query(value ="SELECT p FROM post")
//    Page<Post> findHET(Pageable pageable);

//    Page<Post> findAll(Pageable pageable);

}
