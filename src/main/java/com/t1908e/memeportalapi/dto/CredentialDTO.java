package com.t1908e.memeportalapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialDTO {
    private String accessToken;
    private String refreshToken;
    private UserDTO user;
}
