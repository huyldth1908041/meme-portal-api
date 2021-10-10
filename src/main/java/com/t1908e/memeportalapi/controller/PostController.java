package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;

import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.service.PostService;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RESTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@CrossOrigin
public class PostController {
    private final PostService postService;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<?> createPost(
            @Valid @RequestBody PostDTO.CreatePostDTO createData,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save post failed");
        }
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(new RESTResponse
                    .CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("Required token in header")
                    .build());
        }

        String accessToken = token.replace("Bearer", "").trim();
        DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(accessToken);
        String username = decodedJWT.getSubject();
        return postService.savePost(createData, username);
    }

    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<?> searchPosts(
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "title", required = false) String title,
            @RequestParam(name = "creatorId", required = false) Integer creatorId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        HashMap<String, Object> params = new HashMap<>();
        if (status != null) {
            params.put("status", status);
        }
        if (categoryId != null) {
            params.put("categoryId", categoryId);
        }
        if (title != null) {
            params.put("title", title);
        }
        if (creatorId != null) {
            params.put("userId", creatorId);
        }
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "id";
        return postService.searchListPost(params, page - 1, limit, sortBy, order);
    }

    @RequestMapping(value = "/verify", method = RequestMethod.POST)
    public ResponseEntity<?> verifyPosts(@RequestBody @Valid PostDTO.VerifyPostDTO verifyPostDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "Save post failed");
        }
        return postService.verifyPosts(verifyPostDTO.getPostIds());

    }

}
