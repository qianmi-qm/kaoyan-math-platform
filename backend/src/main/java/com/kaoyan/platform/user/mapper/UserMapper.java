package com.kaoyan.platform.user.mapper;

import com.kaoyan.platform.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    java.util.List<User> selectAll();

    User selectByEmail(@Param("email") String email);

    int insert(User user);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateEmail(@Param("id") Long id, @Param("email") String email);

    int markEmailVerified(@Param("id") Long id);

    int updateRole(@Param("id") Long id, @Param("role") String role);

    /** 一条 SQL 同时改密码和角色，避免出现“密码改了但角色没变”的中间态 */
    int updatePasswordAndRole(@Param("id") Long id,
                              @Param("passwordHash") String passwordHash,
                              @Param("role") String role);

    /** 改密/登出：递增 token_version，让该用户所有旧 JWT 失效 */
    int bumpTokenVersion(@Param("id") Long id);

    /** 关闭 demo 时清掉 known demo 密码，避免“关了开关仍能登录” */
    int clearDemoUser(@Param("id") Long id);
}
