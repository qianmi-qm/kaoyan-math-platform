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

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUser currentUser) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectByUsername(request.getUsername().trim());
        if (user == null || !StringUtils.hasText(user.getPasswordHash())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        return buildResponse(user);
    }

    public UserVO me() {
        User user = userMapper.selectById(currentUser.getUserId());
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return new UserVO(user.getId(), user.getUsername());
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generate(user.getId(), user.getUsername());
        return new AuthResponse(token, "Bearer", jwtService.getExpireSeconds(),
                user.getId(), user.getUsername());
    }
}
