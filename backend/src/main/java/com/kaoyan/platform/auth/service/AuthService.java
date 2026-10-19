package com.kaoyan.platform.auth.service;

import com.kaoyan.platform.auth.dto.AuthResponse;
import com.kaoyan.platform.auth.dto.LoginRequest;
import com.kaoyan.platform.auth.dto.RefreshRequest;
import com.kaoyan.platform.auth.dto.RegisterRequest;
import com.kaoyan.platform.auth.dto.UserVO;
import com.kaoyan.platform.auth.security.JwtService;
import com.kaoyan.platform.auth.security.LoginUser;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
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
    private final String dummyHash;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUser currentUser,
                       AuthRateLimiter rateLimiter) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
        this.dummyHash = passwordEncoder.encode("timing-equalizer-not-a-real-password");
    }

    public AuthResponse register(RegisterRequest request, String ip) {
        rateLimiter.checkRegister(ip);
        String username = request.getUsername().trim();
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
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
        return new UserVO(user.getId(), user.getUsername(), user.getRole());
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

    private AuthResponse buildResponse(User user) {
        int version = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        String token = jwtService.generate(user.getId(), user.getUsername(), version);
        String refreshToken = jwtService.generateRefresh(user.getId(), user.getUsername(), version);
        return new AuthResponse(token, refreshToken, "Bearer", jwtService.getExpireSeconds(),
                user.getId(), user.getUsername(), user.getRole());
    }
}
