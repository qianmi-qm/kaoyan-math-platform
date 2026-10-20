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

    /** 两步自评：只更新还没自评（is_correct IS NULL）且已提交的记录 */
    int updateSelfEval(@Param("id") Long id, @Param("isCorrect") boolean isCorrect);

    /** 按用户+日期查记录（联合索引 idx_user_date 完全命中） */
    List<PracticeRecord> selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    List<PracticeRecord> selectByUserAndDateRange(@Param("userId") Long userId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to);

    /** 掌握度输入：某题最近 N 次作答（is_correct 非 null） */
    List<PracticeRecord> selectRecentByQuestion(@Param("userId") Long userId,
                                                @Param("questionId") Long questionId,
                                                @Param("limit") int limit);

    /** 批量版：每道题各取最近 N 次（调用方保证 questionIds 非空） */
    List<PracticeRecord> selectRecentByQuestionIds(@Param("userId") Long userId,
                                                   @Param("questionIds") List<Long> questionIds,
                                                   @Param("limit") int limit);

    /** 每日汇总：总题数 / 正确数 / 平均用时（L4 聚合 SQL 主战场） */
    DailyAgg selectDailyAgg(@Param("userId") Long userId, @Param("date") LocalDate date);

    @lombok.Data
    class DailyAgg {
        private Integer totalCount;
        private Integer submittedCount;
        private Integer gradedCount;
        private Integer correctCount;
        private Integer avgDurationSec;
    }
}
