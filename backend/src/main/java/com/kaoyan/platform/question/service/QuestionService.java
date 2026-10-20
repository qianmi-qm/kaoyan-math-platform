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
     * Excel 批量导入：EasyExcel 流式逐行读，按 contentHash 幂等。
     * 每行校验题型/难度/知识点；失败行收集行号和原因，不中断整个文件。
     * 事务边界：整个文件一个事务，坏行在方法内 catch，不触发回滚。
     */
    ImportResultVO importExcel(MultipartFile file) throws IOException;

    PageVO<QuestionVO> page(QuestionQueryDTO query);

    /** 详情：带答案与解析，供复盘/自评场景 */
    QuestionVO detail(Long id);

    /** 拍照搜题同类题：按知识点名找题，找不到则随机兜底 */
    java.util.List<QuestionVO> similar(java.util.List<String> kpNames, int limit);
}
