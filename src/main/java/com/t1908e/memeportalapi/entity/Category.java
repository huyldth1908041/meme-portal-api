package com.t1908e.memeportalapi.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@NoArgsConstructor
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private int status;
    private Date createdAt;
    private Date updatedAt;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "category", fetch = FetchType.LAZY)
    private Set<Post> posts;

    public Category(String name) {
        this.name = name;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = 1;
    }

}
