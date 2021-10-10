package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.CommentLike;

import java.util.Date;

public class CommentLikeDTO {
    private int id;
    private Date likedAt;
    private long userId;
    private int commentId;

    public CommentLikeDTO(CommentLike commentLike) {
        this.id = commentLike.getId();
        this.likedAt = commentLike.getLikedAt();
        this.userId = commentLike.getUserId();
        this.commentId = commentLike.getCommentId();
    }
}
