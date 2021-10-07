package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Account;
import com.t1908e.memeportalapi.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private long id;
    private String username;
    private String role;
    private int status;
    private String phone;
    private String fullName;
    private Date birthday;
    private int gender;
    private double tokenBalance;
    private String displayNameColor;
    private Date createdAt;
    private Date updatedAt;
    //list post,...

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getAccount().getUsername();
        this.role = user.getAccount().getRole().getName();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.gender = user.getGender();
        this.phone = user.getPhone();
        this.tokenBalance = user.getTokenBalance();
        this.birthday = user.getBirthDay();
        this.displayNameColor = user.getDisplayNameColor();
    }
}
