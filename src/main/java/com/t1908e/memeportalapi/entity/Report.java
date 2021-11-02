package com.t1908e.memeportalapi.entity;

import com.t1908e.memeportalapi.enums.ReportType;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.ORDINAL)
    private ReportType type; //1 user report | 2 post report
    private int targetId;
    private String content;
    private Date createdAt;
    private Date updatedAt;
    private int status;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE,CascadeType.PERSIST,CascadeType.REFRESH})
    @JoinColumn(name = "userId")
    private User user;
    @Column(updatable = false, insertable = false)
    private long userId;
}
