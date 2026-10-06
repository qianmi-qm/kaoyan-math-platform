package com.kaoyan.platform.report.mapper;

import com.kaoyan.platform.report.entity.DailyReport;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DailyReportMapper {

    /**
     * 幂等落库：一天一份报告。
     * L4 提示：UNIQUE(user_id, report_date) 已建，用 INSERT ... ON DUPLICATE KEY UPDATE
     * 比先查再插好在哪？（并发下不会插出两份——决策日志素材）
     */
    int upsert(DailyReport report);

    DailyReport selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 趋势图数据源 */
    List<DailyReport> selectRange(@Param("userId") Long userId,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to);
}
