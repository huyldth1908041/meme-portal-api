package com.t1908e.memeportalapi.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private String content;
    private double amount;
    private Date createdAt;
    private Date updatedAt;
    private int status;

    public Invoice(String name, String content, double amount, User user) {
        this.name = name;
        this.content = content;
        this.amount = amount;
        this.user = user;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = 1;
    }

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "userId")
    private User user;
    @Column(insertable = false, updatable = false)
    private long userId;

}
