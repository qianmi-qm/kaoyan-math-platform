package com.kaoyan.platform.user.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private LocalDateTime createdAt;
}
