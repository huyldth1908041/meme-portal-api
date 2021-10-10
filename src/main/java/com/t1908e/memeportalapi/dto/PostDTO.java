package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Category;
import com.t1908e.memeportalapi.entity.Comment;
import com.t1908e.memeportalapi.entity.Post;
import com.t1908e.memeportalapi.entity.PostLike;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class PostDTO {
    private int id;
    private String title;
    private String description;
    private String image;
    private double upHotTokenNeeded;
    private int status;
    private Date createdAt;
    private Date updatedAt;

    private UserDTO userDTO;

    private Category category;

    private Set<PostLikeDTO> postLikeSet;

    private  Set<CommentDTO> commentSet;

    public PostDTO( Post post){
        this.id = post.getId();
        this.title = post.getTitle();
        this.description = post.getDescription();
        this.image = post.getImage();
        this.upHotTokenNeeded = post.getUpHotTokenNeeded();
        this.status = post.getStatus();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.userDTO = new UserDTO(post.getUser());
        this.category = post.getCategory();
        this.postLikeSet =  new HashSet<>();
        post.getPostLikes().forEach(item -> this.postLikeSet.add(new PostLikeDTO(item)));
        this.commentSet = new HashSet<>();
        post.getComments().forEach(item -> this.commentSet.add(new CommentDTO(item)));
    }
}
