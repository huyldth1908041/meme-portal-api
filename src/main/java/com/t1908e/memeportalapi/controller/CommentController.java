package com.t1908e.memeportalapi.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.t1908e.memeportalapi.dto.CommentDTO;
import com.t1908e.memeportalapi.dto.PostLikeDTO;
import com.t1908e.memeportalapi.service.CommentService;
import com.t1908e.memeportalapi.util.JwtUtil;
import com.t1908e.memeportalapi.util.RESTResponse;
import com.t1908e.memeportalapi.util.RESTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/comment")
@RequiredArgsConstructor
@CrossOrigin
public class CommentController {
    private final CommentService commentService;

    @RequestMapping(value = "", method = RequestMethod.POST)
    public ResponseEntity<?> saveComment(@RequestBody @Valid CommentDTO.CreateCommentDTO createCommentDTO,
                                          BindingResult bindingResult,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {
        if (bindingResult.hasErrors()) {
            return RESTUtil.getValidationErrorsResponse(bindingResult, "update post failed");
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
        return commentService.saveComment(createCommentDTO, username);

    }
}
