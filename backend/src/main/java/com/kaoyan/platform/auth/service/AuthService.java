package com.kaoyan.platform.auth.service;

import com.kaoyan.platform.auth.dto.AuthResponse;
import com.kaoyan.platform.auth.dto.LoginRequest;
import com.kaoyan.platform.auth.dto.RegisterRequest;
import com.kaoyan.platform.auth.dto.UserVO;
import com.kaoyan.platform.auth.security.JwtService;
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

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUser currentUser,
                       AuthRateLimiter rateLimiter) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
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
        if (user == null || !StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
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

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generate(user.getId(), user.getUsername());
        return new AuthResponse(token, "Bearer", jwtService.getExpireSeconds(),
                user.getId(), user.getUsername(), user.getRole());
    }
}
