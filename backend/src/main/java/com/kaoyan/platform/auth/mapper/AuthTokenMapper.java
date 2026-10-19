package com.kaoyan.platform.auth.mapper;

import com.kaoyan.platform.auth.entity.AuthToken;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthTokenMapper {

    int insert(AuthToken token);

    AuthToken selectByHash(@Param("tokenHash") String tokenHash);

    /** 只有未使用时才能标记成功，防并发重复消费 */
    int markUsed(@Param("tokenHash") String tokenHash, @Param("usedAt") LocalDateTime usedAt);
}
