package com.kaoyan.platform.question.service;

import com.kaoyan.platform.common.result.PageVO;
import com.kaoyan.platform.question.dto.QuestionCreateDTO;
import com.kaoyan.platform.question.dto.QuestionQueryDTO;
import com.kaoyan.platform.question.vo.ImportResultVO;
import com.kaoyan.platform.question.vo.QuestionVO;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {

    QuestionVO create(QuestionCreateDTO dto);

    /** 管理员编辑题目：覆盖题干/选项/答案/解析/难度/知识点。 */
    QuestionVO update(Long id, QuestionCreateDTO dto);

    /**
     * L2 学习任务：Excel 批量导入。
     * 要求：
     * 1. EasyExcel 流式逐行读取（QuestionImportRow），不要一次性全load进内存
     * 2. 每行校验：必填、题型/难度范围、知识点名称可解析（逗号分隔 kpNames -> 按名查 id）
     * 3. 幂等：按 contentHash 查重，重复行计入 duplicated 不报错
     * 4. 合法行分批入库（先 question 再 question_kp），失败行记录"行号 + 原因"返回
     * 5. 思考事务边界：整文件一个事务还是每批一个？（记入决策日志）
     */
    ImportResultVO importExcel(MultipartFile file) throws IOException;

    PageVO<QuestionVO> page(QuestionQueryDTO query);

    /** 详情：带答案与解析，供复盘/自评场景 */
    QuestionVO detail(Long id);

    /** 拍照搜题同类题：按知识点名找题，找不到则随机兜底 */
    java.util.List<QuestionVO> similar(java.util.List<String> kpNames, int limit);
}
