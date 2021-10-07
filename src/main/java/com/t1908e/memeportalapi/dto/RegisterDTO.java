package com.t1908e.memeportalapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterDTO {
    @NotBlank(message = "username is required")
    private String username;
    @NotBlank(message = "phone is required")
    private String phone;
    @NotBlank(message = "fullName is required")
    private String fullName;
    @NotBlank(message = "password is required")
    @Size(min = 6, message = "password is too short")
    private String password;
}
