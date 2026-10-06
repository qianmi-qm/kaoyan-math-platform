package com.kaoyan.platform.practice.mapper;

import com.kaoyan.platform.practice.entity.PracticeRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PracticeRecordMapper {

    /** 批量插入一次练习的记录（started_at=now，submitted_at 为空） */
    int insertBatch(@Param("records") List<PracticeRecord> records);

    PracticeRecord selectById(@Param("id") Long id);

    int updateSubmit(PracticeRecord record);

    /** 按用户+日期查记录（联合索引 idx_user_date 完全命中） */
    List<PracticeRecord> selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 掌握度输入：某题最近 N 次作答（is_correct 非 null） */
    List<PracticeRecord> selectRecentByQuestion(@Param("userId") Long userId,
                                                @Param("questionId") Long questionId,
                                                @Param("limit") int limit);

    /** 每日汇总：总题数 / 正确数 / 平均用时（L4 聚合 SQL 主战场） */
    DailyAgg selectDailyAgg(@Param("userId") Long userId, @Param("date") LocalDate date);

    @lombok.Data
    class DailyAgg {
        private Integer totalCount;
        private Integer correctCount;
        private Integer avgDurationSec;
    }
}
