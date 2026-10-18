package com.kaoyan.platform.ai.mapper;

import com.kaoyan.platform.ai.entity.AiExplanation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiExplanationMapper {

    AiExplanation selectByUserQuestionAndAnswer(@Param("userId") Long userId,
                                                @Param("questionId") Long questionId,
                                                @Param("answerHash") String answerHash);

    /** 同一用户同一题同一答案只保留一份讲解 */
    int upsert(AiExplanation explanation);
}
