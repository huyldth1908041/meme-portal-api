package com.t1908e.memeportalapi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDTO {
    private String content;
    private String thumbnail;
    private int status;
    private Date createdAt;
    private String url;
}
