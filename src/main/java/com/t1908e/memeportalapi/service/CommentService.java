package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.CommentDTO;
import com.t1908e.memeportalapi.dto.PostLikeDTO;
import com.t1908e.memeportalapi.entity.Comment;
import com.t1908e.memeportalapi.entity.PostLike;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.CommentRepository;
import com.t1908e.memeportalapi.repository.UserRepository;
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
public class CommentService {
    private final CommentRepository commentRepository;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;

    public ResponseEntity<?> saveComment(CommentDTO.CreateCommentDTO createCommentDTO, String username){

        HashMap<String, Object> restResponse = new HashMap<>();
        if (username.isEmpty() || createCommentDTO == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage("Null pointer exception").build();
            return ResponseEntity.badRequest().body(restResponse);
        }


        User creator = authenticationService.getAppUser(username);
        if (creator == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }

        Comment newComment = new Comment();
        newComment.setContent(createCommentDTO.getContent());
        newComment.setUserId(createCommentDTO.getUserId());
        newComment.setRepliedCommentId(createCommentDTO.getRepliedCommentId());
        newComment.setStatus(1);
        newComment.setPostId(createCommentDTO.getPostId());
        if(createCommentDTO.getCreatedAt() == null){
            newComment.setCreatedAt(new Date());
        }else {
            newComment.setCreatedAt(createCommentDTO.getCreatedAt());
        }
        if(createCommentDTO.getUpdatedAt() == null){
            newComment.setUpdatedAt(new Date());
        }else {
            newComment.setUpdatedAt(createCommentDTO.getCreatedAt());
        }

        try {
            Comment savedComment= commentRepository.save(newComment);
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new CommentDTO(savedComment)).build();
            // transfer token here
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
