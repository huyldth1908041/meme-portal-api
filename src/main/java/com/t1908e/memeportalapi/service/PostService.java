package com.t1908e.memeportalapi.service;


import com.t1908e.memeportalapi.dto.PostDetailDTO;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;


@Component(value = "PostService")
@Service
public class PostService {
    @Autowired
    PostRepository postRepository;


    public Page<PostDetailDTO> getListPost(Pageable pageable) {
        return postRepository.findAll(pageable).map(item -> new PostDetailDTO(item));
    }

    public Post create(PostDetailDTO postDetailDTO) {
        return postRepository.save(new Post());
    }


}
