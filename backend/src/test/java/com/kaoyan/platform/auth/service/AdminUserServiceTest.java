package com.kaoyan.platform.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kaoyan.platform.auth.dto.UpdateRoleRequest;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.user.entity.User;
import com.kaoyan.platform.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private CurrentUser currentUser;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userMapper, currentUser);
    }

    @Test
    @DisplayName("用户列表：返回角色")
    void listsUsers() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setRole("USER");
        when(userMapper.selectAll()).thenReturn(List.of(user));

        assertEquals("bob", service.list().get(0).getUsername());
        assertEquals("USER", service.list().get(0).getRole());
    }

    @Test
    @DisplayName("不能修改自己的角色")
    void cannotChangeOwnRole() {
        when(currentUser.getUserId()).thenReturn(1L);
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole("USER");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.updateRole(1L, request));
        assertEquals(ErrorCode.PARAM_ERROR, e.getErrorCode());
    }

    @Test
    @DisplayName("可以修改其他用户角色")
    void changesOtherRole() {
        when(currentUser.getUserId()).thenReturn(1L);
        User target = new User();
        target.setId(2L);
        when(userMapper.selectById(2L)).thenReturn(target);
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRole("ADMIN");

        service.updateRole(2L, request);

        verify(userMapper).updateRole(2L, "ADMIN");
    }
}
