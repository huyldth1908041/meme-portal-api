package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Post;
import org.springframework.data.repository.PagingAndSortingRepository;


public interface PostRepository extends PagingAndSortingRepository<Post,Integer> {
//    @Query(value ="SELECT p FROM post")
//    Page<Post> findHET(Pageable pageable);

//    Page<Post> findAll(Pageable pageable);

}
