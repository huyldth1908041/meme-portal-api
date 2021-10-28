package com.t1908e.memeportalapi.entity;

import com.t1908e.memeportalapi.enums.TransactionType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.ORDINAL)
    private TransactionType type;
    private String reason;
    private double amount;
    private String verifyCode;
    private int status; //0 pending -1 | deleted | 1 active
    private Date createdAt;
    private Date updatedAt;
    private long targetId; //user id, post id, badgeId, displayColorId, adsID

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinColumn(name = "userId")
    private User user;
    @Column(insertable = false, updatable = false)
    private long userId;
}
