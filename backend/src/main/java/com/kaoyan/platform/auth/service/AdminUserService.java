package com.kaoyan.platform.auth.service;

import com.kaoyan.platform.auth.dto.AdminUserVO;
import com.kaoyan.platform.auth.dto.UpdateRoleRequest;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {

    private final UserMapper userMapper;
    private final CurrentUser currentUser;

    public AdminUserService(UserMapper userMapper, CurrentUser currentUser) {
        this.userMapper = userMapper;
        this.currentUser = currentUser;
    }

    public List<AdminUserVO> list() {
        return userMapper.selectAll().stream()
                .map(user -> new AdminUserVO(user.getId(), user.getUsername(),
                        user.getRole(), user.getCreatedAt()))
                .toList();
    }

    public void updateRole(Long userId, UpdateRoleRequest request) {
        if (currentUser.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能修改自己的角色");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        userMapper.updateRole(userId, request.getRole());
    }
}
