package com.kaoyan.platform.auth.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateRoleRequest {

    @Pattern(regexp = "USER|ADMIN", message = "角色只能是 USER 或 ADMIN")
    private String role;
}
