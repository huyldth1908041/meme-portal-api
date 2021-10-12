package com.t1908e.memeportalapi.dto;

import com.t1908e.memeportalapi.entity.Account;
import com.t1908e.memeportalapi.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private long id;
    private String username;
    private String role;
    private String avatar;
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
        this.avatar = user.getAvatar();
        this.fullName = user.getFullName();
    }

    @Data
    @NoArgsConstructor
    public static class UpdateUserProfileDTO {
        public static final String DATE_FORMAT = "dd-MM-yyyy";

        @NotBlank(message = "avatar is required")
        private String avatar;
        @NotBlank(message = "phone is required")
        private String phone;
        @NotBlank(message = "full name is required")
        private String fullName;
        @NotBlank(message = "birthday is required")
        private String birthday;
        @NotNull(message = "gender is required")
        private int gender;
    }
}
