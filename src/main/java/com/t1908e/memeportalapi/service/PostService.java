package com.t1908e.memeportalapi.service;


import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Set;


@Component(value = "PostService")
@Service
public class PostService {
    @Autowired
    PostRepository postRepository;


    public Page<PostDTO> getListPost(Pageable pageable) {
        return postRepository.findAll(pageable).map(item -> new PostDTO(item));
    }

    public Post create(PostDTO postDTO) {
        return postRepository.save(new Post());
    }


}
