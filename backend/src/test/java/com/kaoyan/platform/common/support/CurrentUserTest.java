package com.kaoyan.platform.common.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.kaoyan.platform.auth.security.LoginUser;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CurrentUser 从 SecurityContext 读取用户，不再固定 user_id=1")
    void readsLoginUserFromContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new LoginUser(7L, "alice"), null, List.of()));

        assertEquals(7L, currentUser.getUserId());
        assertEquals("alice", currentUser.getLoginUser().username());
    }

    @Test
    @DisplayName("未登录访问业务层 → 401 业务异常")
    void unauthenticatedThrows() {
        BusinessException e = assertThrows(BusinessException.class, currentUser::getUserId);
        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }
}
