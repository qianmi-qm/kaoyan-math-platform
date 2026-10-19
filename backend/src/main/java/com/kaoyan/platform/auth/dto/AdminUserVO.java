package com.kaoyan.platform.auth.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserVO {

    private Long id;
    private String username;
    private String role;
    private LocalDateTime createdAt;
}
