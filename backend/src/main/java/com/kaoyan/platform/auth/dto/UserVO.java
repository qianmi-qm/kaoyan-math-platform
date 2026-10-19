package com.kaoyan.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String role;
    private String email;
    private Boolean emailVerified;
}
