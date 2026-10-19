package com.kaoyan.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresIn;
    private Long userId;
    private String username;
}
