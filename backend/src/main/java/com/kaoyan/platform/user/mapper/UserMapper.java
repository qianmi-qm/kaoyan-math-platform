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
}
