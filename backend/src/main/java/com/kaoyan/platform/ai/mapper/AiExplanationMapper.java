package com.kaoyan.platform.ai.mapper;

import com.kaoyan.platform.ai.entity.AiExplanation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiExplanationMapper {

    AiExplanation selectByUserAndQuestion(@Param("userId") Long userId,
                                          @Param("questionId") Long questionId);

    /** 同一用户同一题只保留一份讲解 */
    int upsert(AiExplanation explanation);
}
