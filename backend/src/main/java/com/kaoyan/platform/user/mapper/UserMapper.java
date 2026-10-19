package com.kaoyan.platform.user.mapper;

import com.kaoyan.platform.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int insert(User user);

    int updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updateRole(@Param("id") Long id, @Param("role") String role);

    /** 关闭 demo 时清掉 known demo 密码，避免“关了开关仍能登录” */
    int clearDemoUser(@Param("id") Long id);
}
