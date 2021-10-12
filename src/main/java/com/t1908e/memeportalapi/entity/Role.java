package com.t1908e.memeportalapi.entity;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private Date createdAt;
    private Date updatedAt;
    private int status; //1 Active | 0 deactive

    @OneToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH}, mappedBy = "role", fetch = FetchType.LAZY)
    private Set<Account> accounts = new HashSet<Account>();

    public Role(String name) {
        this.name = name;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = 1;
    }
}
