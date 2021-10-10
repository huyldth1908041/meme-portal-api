package com.t1908e.memeportalapi.service;



import ch.qos.logback.core.pattern.Converter;
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

}
