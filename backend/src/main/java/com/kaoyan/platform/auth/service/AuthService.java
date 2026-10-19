package com.kaoyan.platform.auth.service;

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
import com.kaoyan.platform.auth.security.JwtService;
import com.kaoyan.platform.auth.security.LoginUser;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
import java.time.Duration;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUser currentUser;
    private final AuthRateLimiter rateLimiter;
    private final AuthTokenService tokenService;
    private final AuthMailService mailService;
    private final String dummyHash;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUser currentUser,
                       AuthRateLimiter rateLimiter, AuthTokenService tokenService,
                       AuthMailService mailService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    public AuthResponse register(RegisterRequest request, String ip) {
        rateLimiter.checkRegister(ip);
        String username = request.getUsername().trim();
        String email = normalizeEmail(request.getEmail());
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (email != null && userMapper.selectByEmail(email) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被使用");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setEmail(email);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名或邮箱已存在");
        }
        if (email != null) {
            sendVerify(user);
        }
        rateLimiter.onRegister(ip);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request, String ip) {
        String username = request.getUsername().trim();
        rateLimiter.checkLogin(username, ip);
        User user = userMapper.selectByUsername(username);
        boolean hasHash = user != null && StringUtils.hasText(user.getPasswordHash());
        boolean matched = hasHash && passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!matched) {
            // 用户不存在时也做一次 BCrypt，避免用响应耗时枚举用户名
            if (!hasHash) {
                passwordEncoder.matches(request.getPassword(), dummyHash);
            }
            rateLimiter.onLoginFailure(username, ip);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        rateLimiter.onLoginSuccess(username, ip);
        return buildResponse(user);
    }

    public UserVO me() {
        User user = userMapper.selectById(currentUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return new UserVO(user.getId(), user.getUsername(), user.getRole(),
                user.getEmail(), user.getEmailVerified());
    }

    /** 服务端撤销当前用户所有 JWT；前端清 token 是第二步 */
    public void logout() {
        userMapper.bumpTokenVersion(currentUser.getUserId());
    }

    public AuthResponse refresh(RefreshRequest request) {
        LoginUser parsed = jwtService.parseRefresh(request.getRefreshToken());
        User user = userMapper.selectById(parsed.id());
        int version = user == null || user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        if (user == null || parsed.tokenVersion() != version) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        // refresh token 单次使用：轮换后旧 access/refresh 全部失效
        userMapper.bumpTokenVersion(user.getId());
        user.setTokenVersion(version + 1);
        return buildResponse(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        User user = userMapper.selectById(currentUser.getUserId());
        if (user == null || !StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "原密码错误");
        }
        userMapper.updatePasswordHash(user.getId(), passwordEncoder.encode(request.getNewPassword()));
    }

    /** 无论邮箱是否存在都返回同一结果，避免枚举邮箱。 */
    public void forgotPassword(ForgotPasswordRequest request, String ip) {
        String email = normalizeEmail(request.getEmail());
        rateLimiter.checkForgot(email, ip);
        User user = userMapper.selectByEmail(email);
        if (user != null) {
            String token = tokenService.create(user.getId(), AuthTokenService.RESET_PASSWORD,
                    Duration.ofMinutes(30));
            mailService.sendReset(email, token);
        }
        rateLimiter.onForgot(email, ip);
    }

    public void resetPassword(ResetPasswordRequest request) {
        Long userId = tokenService.consume(request.getToken(), AuthTokenService.RESET_PASSWORD);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        userMapper.updatePasswordHash(userId, passwordEncoder.encode(request.getNewPassword()));
        userMapper.markEmailVerified(userId);
    }

    public void verifyEmail(VerifyEmailRequest request) {
        Long userId = tokenService.consume(request.getToken(), AuthTokenService.VERIFY_EMAIL);
        userMapper.markEmailVerified(userId);
    }

    public void updateEmail(UpdateEmailRequest request) {
        String email = normalizeEmail(request.getEmail());
        User current = userMapper.selectById(currentUser.getUserId());
        if (current == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        User other = userMapper.selectByEmail(email);
        if (other != null && !other.getId().equals(current.getId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被使用");
        }
        userMapper.updateEmail(current.getId(), email);
        current.setEmail(email);
        current.setEmailVerified(false);
        sendVerify(current);
    }

    public void resendVerification() {
        User current = userMapper.selectById(currentUser.getUserId());
        if (current == null || !StringUtils.hasText(current.getEmail())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请先设置邮箱");
        }
        if (Boolean.TRUE.equals(current.getEmailVerified())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱已验证");
        }
        sendVerify(current);
    }

    private void sendVerify(User user) {
        if (!StringUtils.hasText(user.getEmail())) {
            return;
        }
        String token = tokenService.create(user.getId(), AuthTokenService.VERIFY_EMAIL,
                Duration.ofHours(24));
        mailService.sendVerify(user.getEmail(), token);
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private AuthResponse buildResponse(User user) {
        int version = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String token = jwtService.generate(user.getId(), user.getUsername(), version);
        String refreshToken = jwtService.generateRefresh(user.getId(), user.getUsername(), version);
        return new AuthResponse(token, refreshToken, "Bearer", jwtService.getExpireSeconds(),
                user.getId(), user.getUsername(), user.getRole());
    }
}
