package com.kaoyan.platform.auth.security;

/** SecurityContext 中的当前登录用户，不直接暴露密码字段。 */
public record LoginUser(Long id, String username, int tokenVersion) {

    public LoginUser(Long id, String username) {
        this(id, username, 0);
    }
}
