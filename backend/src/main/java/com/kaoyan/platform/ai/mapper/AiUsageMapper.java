package com.kaoyan.platform.ai.mapper;

import com.kaoyan.platform.ai.entity.AiUsage;
import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiUsageMapper {

    /**
     * 原子占一次配额：插入返回 1；更新且 count < limit 返回 2；
     * 已达上限返回 0。
     */
    int tryConsume(@Param("userId") Long userId,
                   @Param("date") LocalDate date,
                   @Param("limit") int limit);

    AiUsage selectByUserAndDate(@Param("userId") Long userId,
                                @Param("date") LocalDate date);

    int addTokens(@Param("userId") Long userId,
                  @Param("date") LocalDate date,
                  @Param("tokens") int tokens);

    int refund(@Param("userId") Long userId, @Param("date") LocalDate date);
}
