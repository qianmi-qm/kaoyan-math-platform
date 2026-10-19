package com.kaoyan.platform.auth.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AuthToken {

    private Long id;
    private Long userId;
    private String type;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;
}
