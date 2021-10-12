package com.t1908e.memeportalapi.service;


import ch.qos.logback.core.pattern.Converter;
import com.t1908e.memeportalapi.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.UserDTO;
import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.CategoryRepository;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.specification.PostSpecification;
import com.t1908e.memeportalapi.specification.PostSpecificationBuilder;
import com.t1908e.memeportalapi.specification.SearchCriteria;
import com.t1908e.memeportalapi.util.RESTResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (creator.getAccount().getRole().getName().equals("admin")) {
            newPost.setStatus(1); // admin create post no need to verify
        } else {
            newPost.setStatus(0); // 0 -> PENDING
        }
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

    public ResponseEntity<?> editPost(PostDTO.CreatePostDTO postDTO, String creatorUsername, int postId) {
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
        User editor = authenticationService.getAppUser(creatorUsername);
        if (editor == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        if (editor.getAccount().getRole().getName().equals("user")) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Permission denied").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<Post> postOptional = postRepository.findById(postId);
        if (!postOptional.isPresent()) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .setMessage("Post not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Post existPost = postOptional.get();
        existPost.setTitle(postDTO.getTitle());
        existPost.setDescription(postDTO.getDescription());
        existPost.setImage(postDTO.getImage());
        existPost.setUpdatedAt(new Date());
        existPost.setCategory(postCategory);
        try {
            Post savedPost = postRepository.save(existPost);
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

    public ResponseEntity<?> searchListPost(
            HashMap<String, Object> params,
            Integer page,
            Integer limit,
            String sortBy,
            String order
    ) {
        HashMap<String, Object> restResponse = new HashMap<>();
        PostSpecificationBuilder builder = new PostSpecificationBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            builder.with(key, ":", value);
        }
        Specification<Post> spec = builder.build();
        Sort.Direction direction;
        if (order == null) {
            direction = Sort.Direction.DESC;
        } else if (order.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        } else {
            direction = Sort.Direction.DESC;
        }
        Pageable pageInfo = PageRequest.of(page, limit, Sort.by(direction, sortBy));
        Page<Post> all = postRepository.findAll(spec, pageInfo);
        Page<PostDTO> dtoPage = all.map(new Function<Post, PostDTO>() {
            @Override
            public PostDTO apply(Post post) {
                return new PostDTO(post);
            }
        });
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(dtoPage).build();
        return ResponseEntity.ok().body(restResponse);
    }

    public ResponseEntity<?> verifyPosts(ArrayList<Integer> postIds) {
        int recordsAffected = postRepository.changePostStatus(postIds, 1);// 1 -> ACTIVE
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData("Updated success ".concat(String.valueOf(recordsAffected)).concat(" rows affected")).build();
        return ResponseEntity.ok().body(restResponse);
    }


    public ResponseEntity<?> getPostDetail(int id) {
        HashMap<String, Object> restResponse = new HashMap<>();
        Optional<Post> postOptional = postRepository.findById(id);
        Post post = postOptional.orElse(null);
        if (post == null) {
            restResponse = new RESTResponse.CustomError()
                    .setMessage("This post is not exist or had been delete !")
                    .setCode(HttpStatus.NOT_FOUND.value())
                    .build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData(new PostDTO(post)).build();
        return ResponseEntity.ok().body(restResponse);
    }


    public ResponseEntity<?> deletePosts(ArrayList<Integer> postIds) {
        int recordsAffected = postRepository.changePostStatus(postIds, -1);// -1 -> DE-ACTIVE
        HashMap<String, Object> restResponse = new RESTResponse.Success()
                .setMessage("Ok")
                .setStatus(HttpStatus.OK.value())
                .setData("Updated success ".concat(String.valueOf(recordsAffected)).concat(" rows affected")).build();
        return ResponseEntity.ok().body(restResponse);
    }

}
