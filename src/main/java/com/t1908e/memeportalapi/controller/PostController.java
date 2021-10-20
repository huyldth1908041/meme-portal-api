package com.t1908e.memeportalapi.controller;


import com.auth0.jwt.interfaces.DecodedJWT;

import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.service.PostService;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RESTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
            return RESTUtil.getValidationErrorsResponse(bindingResult, "update post failed");
        }

        return postService.verifyPosts(verifyPostDTO.getPostIds());

    }


    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getPostDetail(@PathVariable(name = "id") int id) {
        return postService.getPostDetail(id);
    }

    @RequestMapping(value = "", method = RequestMethod.DELETE)
    public ResponseEntity<?> deletePosts(@RequestBody @Valid PostDTO.VerifyPostDTO verifyPostDTO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "delete post failed");
        }
        return postService.deletePosts(verifyPostDTO.getPostIds());

    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public ResponseEntity<?> editPost(
            @RequestBody @Valid PostDTO.CreatePostDTO createPostDTO,
            BindingResult bindingResult,
            @PathVariable(name = "id") int id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "edit post failed");
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
        return postService.editPost(createPostDTO, username, id);

    }


    @RequestMapping(value = "/likePost", method = RequestMethod.POST)
    public ResponseEntity<?> likePost(
            @RequestBody PostDTO.SendPostLikeDTO sendPostLikeDTO,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "like post failed");
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
        return postService.LikeAPost(sendPostLikeDTO.getPostId(), username);
    }

    @RequestMapping(value = "/{id}/like", method = RequestMethod.GET)
    public ResponseEntity<?> getPostLikes(
            @PathVariable(name = "id") int id,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            HttpServletRequest request
    ) {
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "id";
        String username = null;
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.replace("Bearer", "").trim();
                DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(token);
                username = decodedJWT.getSubject();
            }
        } catch (Exception exception) {
            HashMap<String, Object> restResponse = new RESTResponse.CustomError()
                    .setMessage("Decode jwt failed")
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }

        return postService.getPostLikes(id, page - 1, limit, sortBy, order, username);
    }

    @RequestMapping(value = "/{id}/likeCount", method = RequestMethod.GET)
    public ResponseEntity<?> getLikeCount(@PathVariable(name = "id") int id, HttpServletRequest request) {
        String username = null;
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.replace("Bearer", "").trim();
                DecodedJWT decodedJWT = JwtUtil.getDecodedJwt(token);
                username = decodedJWT.getSubject();
            }
        } catch (Exception exception) {
            HashMap<String, Object> restResponse = new RESTResponse.CustomError()
                    .setMessage("Decode jwt failed")
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
            return ResponseEntity.internalServerError().body(restResponse);
        }
        return postService.getLikeCount(id, username);
    }

}
