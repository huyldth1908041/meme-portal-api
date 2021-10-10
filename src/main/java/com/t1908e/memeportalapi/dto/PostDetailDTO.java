package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Category;

import java.util.Date;

public class PostDetailDTO {
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

    private  PostLikeDTO postLikeDTO;

    private  int commentCount;
}
