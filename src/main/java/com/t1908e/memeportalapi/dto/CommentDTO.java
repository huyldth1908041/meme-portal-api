package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Comment;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
    private UserDTO user;
    private int likeCount;
    private int replyCount;
    private int repliedCommentId;


    public CommentDTO(Comment comment, int replyCount) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.status = comment.getStatus();
        this.postId = comment.getPost().getId();
        this.user = new UserDTO(comment.getUser());
        this.likeCount = 0;
        if (comment.getCommentLikes() != null) {
            this.likeCount = comment.getCommentLikes().size();
        }
        this.replyCount = replyCount;
        repliedCommentId = comment.getRepliedCommentId();
    }

    @Data
    public static class CreateCommentDTO {
        @NotBlank(message = "Content is required")
        private String content;
        //nullable when comment is not a reply comment
        private Integer replyCommentId;

    }

    @Data
    public static class CommentLikeDTO {
        private int likeCount;
        private boolean hasLikedYet = false;
    }

    @Data
    public static class SendCommentLikeDTO {
        @NotNull(message = "comment id is required")
        private Integer commentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ListCommentLikeDTO {
        private boolean hasLikedYet = false;
        private Page<UserDTO> likedList;
    }
}
