package com.t1908e.memeportalapi.controller;

import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.repository.PostRepository;
import com.t1908e.memeportalapi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/post")
@RequiredArgsConstructor
@CrossOrigin
public class PostController {

    private final PostService postService;

    @RequestMapping(value = "/list",method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getAllPost(
//            @RequestParam(required = false) int category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "3") int size
    ) {

        try {

            Pageable paging = PageRequest.of(page, size);

            Page<PostDTO> pagePosts;
                pagePosts = postService.getListPost(paging);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", pagePosts);
            response.put("currentPage", pagePosts.getNumber());
            response.put("totalItems", pagePosts.getTotalElements());
            response.put("totalPages", pagePosts.getTotalPages());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/create",method = RequestMethod.POST)

    public Post createPost(@RequestBody PostDTO postDTO){
        return postService.create(postDTO);
    }
}
