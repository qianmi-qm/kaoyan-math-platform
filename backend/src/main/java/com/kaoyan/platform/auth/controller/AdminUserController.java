package com.kaoyan.platform.auth.controller;

import com.kaoyan.platform.auth.dto.AdminUserVO;
import com.kaoyan.platform.auth.dto.UpdateRoleRequest;
import com.kaoyan.platform.auth.service.AdminUserService;
import com.kaoyan.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员")
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户列表（ADMIN）")
    @GetMapping("/users")
    public Result<List<AdminUserVO>> users() {
        return Result.ok(adminUserService.list());
    }

    @Operation(summary = "修改用户角色（ADMIN，不能改自己）")
    @PatchMapping("/users/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        adminUserService.updateRole(id, request);
        return Result.ok();
    }
}
