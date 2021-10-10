package com.t1908e.memeportalapi.service;


import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.CategoryRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticationService authenticationService;

    public ResponseEntity<?> savePost(PostDTO.CreatePostDTO postDTO, String creatorUsername) {
        HashMap<String, Object> restResponse = new HashMap<>();
        if (creatorUsername.isEmpty() || postDTO == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage("Null pointer exception").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Category> categoryOptional = categoryRepository.findById(postDTO.getCategoryId());
        if (!categoryOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Category not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Category postCategory = categoryOptional.get();
        User creator = authenticationService.getAppUser(creatorUsername);
        if (creator == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post newPost = new Post();
        newPost.setTitle(postDTO.getTitle());
        newPost.setDescription(postDTO.getDescription());
        newPost.setImage(postDTO.getImage());
        newPost.setUpHotTokenNeeded(0);
        newPost.setStatus(0); // 0 -> PENDING
        newPost.setCreatedAt(new Date());
        newPost.setUpdatedAt(new Date());
        newPost.setCategory(postCategory);
        newPost.setUser(creator);
        try {
            Post savedPost = postRepository.save(newPost);
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new PostDTO(savedPost)).build();
            return ResponseEntity.ok().body(restResponse);

        } catch (Exception exception) {
            exception.printStackTrace();
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage(exception.getMessage()).build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
    }
}
