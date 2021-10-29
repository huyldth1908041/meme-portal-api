package com.t1908e.memeportalapi.controller;


import com.auth0.jwt.interfaces.DecodedJWT;

import com.t1908e.memeportalapi.dto.CommentDTO;
import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.service.CommentService;
import com.t1908e.memeportalapi.service.PostService;
import com.t1908e.memeportalapi.service.SharePostService;
import com.t1908e.memeportalapi.service.TwilioSmsSender;
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


@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@CrossOrigin
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final SharePostService sharePostService;

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
        return postService.likeAPost(sendPostLikeDTO.getPostId(), username);
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

    @RequestMapping(value = "/topCreator", method = RequestMethod.GET)
    public ResponseEntity<?> getTopCreator() {
        return postService.getTopCreator();
    }

    @RequestMapping(value = "/{id}/comments", method = RequestMethod.POST)
    public ResponseEntity<?> commentAPost(
            @PathVariable(value = "id") int id,
            @RequestBody @Valid CommentDTO.CreateCommentDTO createCommentDTO,
            BindingResult bindingResult,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "comment post failed");
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

        return commentService.commentAPost(id, createCommentDTO, username);
    }

    @RequestMapping(value = "/{id}/comments", method = RequestMethod.GET)
    public ResponseEntity<?> getListComments(
            @PathVariable(value = "id") int id,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "createdAt";
        return commentService.getListComments(id, page - 1, limit, sortBy, order);
    }

    @RequestMapping(value = "/comments/{id}/replyComments", method = RequestMethod.GET)
    public ResponseEntity<?> getListReplyComment(
            @PathVariable(value = "id") int id,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "createdAt";
        return commentService.getRepliedCommentOfAComment(id, page - 1, limit, sortBy, order);
    }

    @RequestMapping(value = "/likeComment", method = RequestMethod.POST)
    public ResponseEntity<?> likeComment(
            @RequestBody CommentDTO.SendCommentLikeDTO sendCommentLikeDTO,
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
        return commentService.likeAComment(sendCommentLikeDTO.getCommentId(), username);
    }

    @RequestMapping(value = "/comments/{id}/likeCount", method = RequestMethod.GET)
    public ResponseEntity<?> getCommentLikeCount(@PathVariable(name = "id") int id, HttpServletRequest request) {
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
        return commentService.getLikeCount(id, username);
    }

    @RequestMapping(value = "/comments/{id}/like", method = RequestMethod.GET)
    public ResponseEntity<?> getCommentLikes(
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

        return commentService.getCommentLikes(id, page - 1, limit, sortBy, order, username);
    }

    @RequestMapping(value = "/{id}/share", method = RequestMethod.POST)
    public ResponseEntity<?> shareAPost(
            @PathVariable(name = "id") int id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
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
        return sharePostService.saveShare(username, id);
    }

    @RequestMapping(value = "/{id}/checkShare", method = RequestMethod.POST)
    public ResponseEntity<?> checkShare(
            @PathVariable(name = "id") int id,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token
    ) {
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
        return sharePostService.checkShare(username, id);
    }


    @RequestMapping(value = "/{id}/push", method = RequestMethod.GET)
    public ResponseEntity<?> getPushedList(
            @PathVariable(name = "id") int id,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "order", required = false) String order,
            @RequestParam(name = "sortBy", required = false) String sortBy
    ) {
        if (page == null || page <= 0) page = 1;
        if (limit == null) limit = 30;
        if (sortBy == null) sortBy = "id";

        return postService.getPushedList(id, page - 1, limit, sortBy, order);
    }

}
