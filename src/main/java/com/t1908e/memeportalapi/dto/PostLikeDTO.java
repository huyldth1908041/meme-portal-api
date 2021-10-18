package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.PostLike;
import com.t1908e.memeportalapi.entity.User;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

public class PostLikeDTO {
    private int id;
    private Date createdAt;
    private UserDTO userDTO;
    private int postId;

    public PostLikeDTO(PostLike postLike) {
        this.id = postLike.getId();
        this.createdAt = postLike.getCreatedAt();
        this.userDTO = new UserDTO(postLike.getUser());
        this.postId = postLike.getPostId();
    }

    @Data
    public static class UserLikePostDTO{
        @NotNull(message = "CreatedAt is required")
        private Date createdAt;
        @NotNull(message = "UserId is required")
        private int userId;
        @NotNull(message = "PostId is required")
        private int postId;
    }

}
