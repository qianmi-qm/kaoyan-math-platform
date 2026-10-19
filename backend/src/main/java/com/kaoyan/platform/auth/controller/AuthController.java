package com.kaoyan.platform.auth.controller;

import com.kaoyan.platform.auth.dto.AuthResponse;
import com.kaoyan.platform.auth.dto.ChangePasswordRequest;
import com.kaoyan.platform.auth.dto.ForgotPasswordRequest;
import com.kaoyan.platform.auth.dto.LoginRequest;
import com.kaoyan.platform.auth.dto.RefreshRequest;
import com.kaoyan.platform.auth.dto.RegisterRequest;
import com.kaoyan.platform.auth.dto.ResetPasswordRequest;
import com.kaoyan.platform.auth.dto.UpdateEmailRequest;
import com.kaoyan.platform.auth.dto.UserVO;
import com.kaoyan.platform.auth.dto.VerifyEmailRequest;
import com.kaoyan.platform.auth.service.AuthService;
import com.kaoyan.platform.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册并直接返回 token")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                         HttpServletRequest http) {
        return Result.ok(authService.register(request, clientIp(http)));
    }

    @Operation(summary = "用户名密码登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest http) {
        return Result.ok(authService.login(request, clientIp(http)));
    }

    @Operation(summary = "刷新 token（refresh token 轮换）")
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.me());
    }

    @Operation(summary = "登出：递增 token_version，使旧 JWT 失效")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "修改密码：成功后旧 token 全部失效")
    @PostMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }

    @Operation(summary = "忘记密码：发送重置链接（不暴露邮箱是否存在）")
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                       HttpServletRequest http) {
        authService.forgotPassword(request, clientIp(http));
        return Result.ok();
    }

    @Operation(summary = "重置密码：token 单次有效")
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.ok();
    }

    @Operation(summary = "验证邮箱")
    @PostMapping("/verify-email")
    public Result<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return Result.ok();
    }

    @Operation(summary = "设置/修改邮箱并发送验证邮件")
    @PostMapping("/email")
    public Result<Void> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        authService.updateEmail(request);
        return Result.ok();
    }

    @Operation(summary = "重发邮箱验证邮件")
    @PostMapping("/email/resend")
    public Result<Void> resendVerification() {
        authService.resendVerification();
        return Result.ok();
    }

    private String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
