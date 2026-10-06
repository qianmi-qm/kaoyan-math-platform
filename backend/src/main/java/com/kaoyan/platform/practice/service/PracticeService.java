package com.kaoyan.platform.practice.service;

import com.kaoyan.platform.practice.dto.StartSessionDTO;
import com.kaoyan.platform.practice.dto.SubmitAnswerDTO;
import com.kaoyan.platform.practice.vo.PracticeRecordVO;
import java.time.LocalDate;
import java.util.List;

public interface PracticeService {

    /**
     * L3 学习任务：开始练习。
     * 按 mode 选题（KP/RANDOM/WRONG），为每道题创建 practice_record
     * （started_at = clock 当前时间，is_correct/duration/submitted 为空），
     * 返回的 VO 不带 correctAnswer/analysis。
     */
    List<PracticeRecordVO> startSession(StartSessionDTO dto);

    /**
     * L3 学习任务：提交答案。
     * 关键约束：
     * 1. durationSec 由服务端计算：clock.now() - record.startedAt，绝不信任前端
     * 2. 客观题(1/2) trim 后忽略大小写比对；大题(3) 读 selfEval，为 null 则 isCorrect 置 null 并返回正确答案
     * 3. 答错的题调用 reviewService.enqueueIfWrong(...) 送入复习队列（L5 接入）
     * 4. 重复提交（submitted_at 非空）抛 PARAM_ERROR
     */
    PracticeRecordVO submit(Long recordId, SubmitAnswerDTO dto);

    List<PracticeRecordVO> listByDate(LocalDate date);
}
