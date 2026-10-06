package com.kaoyan.platform.review.mapper;

import com.kaoyan.platform.review.entity.ReviewSchedule;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewScheduleMapper {

    int insert(ReviewSchedule schedule);

    ReviewSchedule selectById(@Param("id") Long id);

    /** 该用户该题是否已有"待复习"记录（status=0） */
    ReviewSchedule selectActive(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 今日到期清单：status=0 且 next_review_date <= today，按到期日升序（最欠的排最前） */
    List<ReviewSchedule> selectDue(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** 更新 stage / next_review_date / status / completed_at */
    int updateProgress(ReviewSchedule schedule);

    long countActive(@Param("userId") Long userId);

    long countDue(@Param("userId") Long userId, @Param("today") LocalDate today);

    /** 各周期积压量（stats 接口） */
    List<ReviewScheduleMapper.StageCount> selectStageCounts(@Param("userId") Long userId);

    @lombok.Data
    class StageCount {
        private Integer stage;
        private Long count;
    }
}
