package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Comment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CommentDTO {
    private int id;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private int status;
    private int postId;
    private long userId;
    private Page<CommentDTO> repliedComments;
    private int likeCount;


    public CommentDTO(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.status = comment.getStatus();
        this.postId = comment.getPostId();
        this.userId = comment.getUserId();
    }
}
