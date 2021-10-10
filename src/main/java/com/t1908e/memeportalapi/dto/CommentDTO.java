package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Comment;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class CommentDTO {
    private int id;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private int status;
    private int repliedCommentId;

    private int postId;

    private long userId;

    private Set<CommentLikeDTO> commentLikeDTO;

    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.status = comment.getStatus();
        this.repliedCommentId = comment.getRepliedCommentId();
        this.postId = comment.getPostId();
        this.userId = comment.getUserId();
        //convert set<CommentLike> to set<CommentLikeDTO>
        this.commentLikeDTO = new HashSet<>();
        comment.getCommentLikes().forEach(item -> this.commentLikeDTO.add(new CommentLikeDTO(item)));
    }
}
