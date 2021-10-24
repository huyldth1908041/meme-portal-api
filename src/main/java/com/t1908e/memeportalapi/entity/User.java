package com.t1908e.memeportalapi.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String avatar;
    private String fullName;
    private Date birthDay;
    private String phone;
    private int gender; // 1 male 0 female
    private double tokenBalance;
    private String displayNameColor;
    private Date createdAt;
    private Date updatedAt;
    private int status;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, fetch = FetchType.EAGER)
    @JoinColumn(name = "accountId", referencedColumnName = "id")
    private Account account; // biến account này sẽ trùng  với giá trị  mappedBy trong Class Account
    @Column(insertable = false, updatable = false)
    private long accountId;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Post> posts;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Comment> comments;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<CommentLike> commentLikes;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<PostLike> postLikes;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Notification> notifications;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Report> reports;

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "user", fetch = FetchType.LAZY)
    private Set<Invoice> invoices;

    public double addToken(double amount) {
        double tokenBalance = this.getTokenBalance();
        if (amount < 0) {
            return tokenBalance;
        }
        double newBalance = tokenBalance + amount;
        this.setTokenBalance(newBalance);
        return newBalance;
    }

    public double subtractToken(double amount) {
        double tokenBalance = this.getTokenBalance();
        if (amount < 0) {
            return tokenBalance;
        }
        if (amount > tokenBalance) {
            return tokenBalance;
        }
        double newBalance = tokenBalance - amount;
        this.setTokenBalance(newBalance);
        return newBalance;
    }
}
