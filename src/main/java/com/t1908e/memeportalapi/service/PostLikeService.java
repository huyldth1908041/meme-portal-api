package com.t1908e.memeportalapi.service;

import com.t1908e.memeportalapi.dto.PostDTO;
import com.t1908e.memeportalapi.dto.PostLikeDTO;
import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.PostLike;
import com.t1908e.memeportalapi.entity.User;
import com.t1908e.memeportalapi.repository.CategoryRepository;
import com.t1908e.memeportalapi.repository.PostLikeRepository;
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
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final CategoryRepository categoryRepository;
    private final AuthenticationService authenticationService;

    public ResponseEntity<?> savePostLike(PostLikeDTO.UserLikePostDTO userLikePostDTO,String likeCreatorUsername){

        HashMap<String, Object> restResponse = new HashMap<>();
        if (likeCreatorUsername.isEmpty() || userLikePostDTO == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .setMessage("Null pointer exception").build();
            return ResponseEntity.badRequest().body(restResponse);
        }


        User creator = authenticationService.getAppUser(likeCreatorUsername);
        if (creator == null) {
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.BAD_REQUEST.value())
                    .setMessage("User not found").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        Optional<PostLike> existedPostLike = postLikeRepository.findPostLikeByPostIdAndUserId(userLikePostDTO.getPostId(), userLikePostDTO.getUserId());
        if(existedPostLike.isPresent()){
            restResponse = new RESTResponse.CustomError()
                    .setCode(HttpStatus.CONFLICT.value())
                    .setMessage("User already like post").build();
            return ResponseEntity.badRequest().body(restResponse);
        }
        PostLike newPostLike = new PostLike();
        newPostLike.setPostId(userLikePostDTO.getPostId());
        newPostLike.setUserId(userLikePostDTO.getUserId());
        if(userLikePostDTO.getCreatedAt() == null){
            newPostLike.setCreatedAt(new Date());
        }else {
            newPostLike.setCreatedAt(userLikePostDTO.getCreatedAt());
        }

        try {
            PostLike savedPostLike = postLikeRepository.save(newPostLike);
            restResponse = new RESTResponse.Success()
                    .setMessage("success")
                    .setStatus(HttpStatus.CREATED.value())
                    .setData(new PostLikeDTO(savedPostLike)).build();
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
