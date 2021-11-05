package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Post;
import lombok.*;
import org.springframework.data.domain.Page;

import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private int id;
    private String title;
    private String description;
    private String image;
    private double upHotTokenNeeded;
    private int status;
    private Date createdAt;
    private Date updatedAt;
    private String category;
    private UserDTO creator;
    private int likeCounts;
    private int categoryId;
    private int commentCounts;
    private int shareCounts;
    private int pushCount;


    public PostDTO(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.description = post.getDescription();
        this.image = post.getImage();
        this.upHotTokenNeeded = post.getUpHotTokenNeeded();
        this.status = post.getStatus();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.category = post.getCategory().getName();
        this.categoryId = post.getCategoryId();
        this.likeCounts = 0;
        if (post.getPostLikes() != null) {
            this.likeCounts = post.getPostLikes().size();
        }
        this.commentCounts = 0;
        if (post.getComments() != null) {
            this.commentCounts = post.getComments().size();
        }
        this.shareCounts = 0;
        if (post.getPostShares() != null) {
            this.shareCounts = post.getPostShares().size();
        }
        this.pushCount = 0;
        if (post.getPushHistories() != null) {
            this.pushCount = post.getPushHistories().size();
        }
        this.creator = new UserDTO(post.getUser());
    }


    @Data
    public static class CreatePostDTO {
        @NotBlank(message = "Title is required")
        @Size(max = 70, message = "title max character is 70")
        private String title;
        private String description;
        @NotBlank(message = "Image is required")
        private String image;
        @NotNull(message = "CategoryId is required")
        private int categoryId;
    }

    @Data
    public static class VerifyPostDTO {
        @NotEmpty(message = "post id is required")
        private ArrayList<Integer> postIds = new ArrayList<>();
    }

    @Data
    public static class PostLikeDTO {
        private int likeCount;
        private boolean hasLikedYet = false;
    }

    @Data
    public static class SendPostLikeDTO {
        @NotNull(message = "post id is required")
        private Integer postId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ListPostLikeDTO {
        private boolean hasLikedYet = false;
        private Page<UserDTO> likedList;
    }

    @Data
    public static class PostStatisticDTO {
        private Date createdAt;
        private int postCount;
    }

}
