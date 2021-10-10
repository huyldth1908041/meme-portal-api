package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.PostLike;
import com.t1908e.memeportalapi.entity.User;

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
}
