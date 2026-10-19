package com.kaoyan.platform.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kaoyan.platform.auth.config.SecurityConfig;
import com.kaoyan.platform.auth.controller.AdminUserController;
import com.kaoyan.platform.auth.controller.AuthController;
import com.kaoyan.platform.auth.dto.AuthResponse;
import com.kaoyan.platform.auth.dto.LoginRequest;
import com.kaoyan.platform.auth.security.JwtAuthenticationFilter;
import com.kaoyan.platform.auth.security.JwtService;
import com.kaoyan.platform.auth.security.RestAccessDeniedHandler;
import com.kaoyan.platform.auth.security.RestAuthenticationEntryPoint;
import com.kaoyan.platform.auth.service.AdminUserService;
import com.kaoyan.platform.auth.service.AuthService;
import com.kaoyan.platform.question.controller.QuestionController;
import com.kaoyan.platform.question.service.QuestionService;
import com.kaoyan.platform.question.vo.QuestionVO;
import com.kaoyan.platform.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, QuestionController.class, AdminUserController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class SecurityAccessTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;
    @MockBean
    private AdminUserService adminUserService;
    @MockBean
    private QuestionService questionService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserMapper userMapper;

    @Test
    @DisplayName("无 token 访问受保护接口 → 401")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/unknown"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登录接口允许匿名访问")
    void loginIsPublic() throws Exception {
        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(new AuthResponse("token", "refresh", "Bearer", 7200,
                        1L, "learner", "USER"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"learner\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("普通用户不能创建题目 → 403")
    void userCannotCreateQuestion() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .with(user("bob").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":1,"content":"test","answer":"A","difficulty":1,"kpIds":[1]}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员可以创建题目，普通用户不能看详情答案")
    void adminCanCreateAndUserCannotReadDetail() throws Exception {
        when(questionService.create(any())).thenReturn(new QuestionVO());

        mockMvc.perform(post("/api/questions")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":1,"content":"test","answer":"A","difficulty":1,"kpIds":[1]}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/questions/1").with(user("bob").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("管理员编辑题目：USER 403，ADMIN 200")
    void questionUpdateRequiresAdmin() throws Exception {
        when(questionService.update(any(), any())).thenReturn(new QuestionVO());
        String body = """
                {"type":1,"content":"test","answer":"A","difficulty":1,"kpIds":[1]}
                """;

        mockMvc.perform(put("/api/questions/1")
                        .with(user("bob").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/questions/1")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("管理员用户列表：USER 403，ADMIN 200")
    void adminUserListRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user("bob").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
