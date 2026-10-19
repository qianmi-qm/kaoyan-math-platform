package com.kaoyan.platform.question.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.result.PageVO;
import com.kaoyan.platform.question.dto.QuestionCreateDTO;
import com.kaoyan.platform.question.dto.QuestionImportRow;
import com.kaoyan.platform.question.dto.QuestionQueryDTO;
import com.kaoyan.platform.question.entity.KnowledgePoint;
import com.kaoyan.platform.question.entity.Question;
import com.kaoyan.platform.question.mapper.KnowledgePointMapper;
import com.kaoyan.platform.question.mapper.QuestionKpRow;
import com.kaoyan.platform.question.mapper.QuestionMapper;
import com.kaoyan.platform.question.service.QuestionService;
import com.kaoyan.platform.question.vo.ImportResultVO;
import com.kaoyan.platform.question.vo.QuestionVO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionMapper questionMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final ObjectMapper objectMapper;

    public QuestionServiceImpl(QuestionMapper questionMapper,
                               KnowledgePointMapper knowledgePointMapper,
                               ObjectMapper objectMapper) {
        this.questionMapper = questionMapper;
        this.knowledgePointMapper = knowledgePointMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public QuestionVO create(QuestionCreateDTO dto) {
        List<Long> kpIds = List.copyOf(new LinkedHashSet<>(dto.getKpIds()));
        if (knowledgePointMapper.selectByIds(kpIds).size() != kpIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "存在不存在的知识点 id");
        }
        Question question = new Question();
        question.setType(dto.getType());
        question.setContent(dto.getContent());
        question.setOptions(dto.getOptions());
        question.setAnswer(dto.getAnswer());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        question.setContentHash(sha256(dto.getContent().trim()));
        validateOptions(dto.getOptions());
        try {
            questionMapper.insert(question);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "题目已存在（题干重复）");
        }
        for (Long kpId : kpIds) {
            questionMapper.insertQuestionKp(question.getId(), kpId);
        }
        return toVO(question, true);
    }

    @Override
    @Transactional
    public QuestionVO update(Long id, QuestionCreateDTO dto) {
        Question existing = questionMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在");
        }
        List<Long> kpIds = List.copyOf(new LinkedHashSet<>(dto.getKpIds()));
        if (knowledgePointMapper.selectByIds(kpIds).size() != kpIds.size()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "存在不存在的知识点 id");
        }
        validateOptions(dto.getOptions());
        String hash = sha256(dto.getContent().trim());
        Question duplicate = questionMapper.selectByContentHash(hash);
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new BusinessException(ErrorCode.CONFLICT, "题目已存在（题干重复）");
        }

        existing.setType(dto.getType());
        existing.setContent(dto.getContent());
        existing.setOptions(dto.getOptions());
        existing.setAnswer(dto.getAnswer());
        existing.setAnalysis(dto.getAnalysis());
        existing.setDifficulty(dto.getDifficulty());
        existing.setContentHash(hash);
        questionMapper.update(existing);
        questionMapper.deleteQuestionKp(id);
        for (Long kpId : kpIds) {
            questionMapper.insertQuestionKp(id, kpId);
        }
        return detail(id);
    }

    /**
     * 事务边界取舍：整文件一个事务。
     * 逐行异常在方法内被 catch，坏行不会触发回滚；换来的是"全有或全无"——
     * 中途进程挂掉不会留下半份数据。代价是大文件长时间持锁。
     * 若导入量到万级，应改为分批提交（每批一个事务）。
     */
    @Override
    @Transactional
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        ImportResultVO result = new ImportResultVO();
        Set<String> seenHashes = new HashSet<>();
        try (InputStream in = file.getInputStream()) {
            EasyExcel.read(in, QuestionImportRow.class, new AnalysisEventListener<QuestionImportRow>() {
                @Override
                public void invoke(QuestionImportRow row, AnalysisContext context) {
                    result.setTotalRows(result.getTotalRows() + 1);
                    int excelRow = context.readRowHolder().getRowIndex() + 1;
                    try {
                        importRow(row, seenHashes, result);
                    } catch (RuntimeException e) {
                        result.setFailed(result.getFailed() + 1);
                        result.getFailures().add(new ImportResultVO.FailedRow(excelRow, e.getMessage()));
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        }
        return result;
    }

    private void importRow(QuestionImportRow row, Set<String> seenHashes, ImportResultVO result) {
        if (row.getContent() == null || row.getContent().isBlank()) {
            throw new IllegalArgumentException("题干为空");
        }
        if (row.getType() == null || row.getType() < 1 || row.getType() > 3) {
            throw new IllegalArgumentException("题型必须为 1/2/3");
        }
        if (row.getAnswer() == null || row.getAnswer().isBlank()) {
            throw new IllegalArgumentException("答案为空");
        }
        if (row.getDifficulty() == null || row.getDifficulty() < 1 || row.getDifficulty() > 3) {
            throw new IllegalArgumentException("难度必须为 1/2/3");
        }
        validateOptions(row.getOptions());

        String hash = sha256(row.getContent().trim());
        if (!seenHashes.add(hash) || questionMapper.selectByContentHash(hash) != null) {
            result.setDuplicated(result.getDuplicated() + 1);
            return;
        }

        Question question = new Question();
        question.setType(row.getType());
        question.setContent(row.getContent());
        question.setOptions(row.getOptions());
        question.setAnswer(row.getAnswer());
        question.setAnalysis(row.getAnalysis());
        question.setDifficulty(row.getDifficulty());
        question.setContentHash(hash);
        questionMapper.insert(question);
        for (Long kpId : resolveKpIds(row.getKpNames())) {
            questionMapper.insertQuestionKp(question.getId(), kpId);
        }
        result.setImported(result.getImported() + 1);
    }

    private List<Long> resolveKpIds(String kpNames) {
        if (kpNames == null || kpNames.isBlank()) {
            return List.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String raw : kpNames.split("[,，]")) {
            String name = raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            KnowledgePoint kp = knowledgePointMapper.selectByName(name);
            if (kp == null) {
                throw new IllegalArgumentException("知识点不存在：" + name);
            }
            ids.add(kp.getId());
        }
        return List.copyOf(ids);
    }

    @Override
    public PageVO<QuestionVO> page(QuestionQueryDTO query) {
        int offset = (query.getPage() - 1) * query.getSize();
        List<Question> rows = questionMapper.selectPage(query.getKpId(), offset, query.getSize());
        long total = questionMapper.countTotal(query.getKpId());

        List<Long> ids = rows.stream().map(Question::getId).toList();
        Map<Long, List<Long>> kpIdsByQuestion = new HashMap<>();
        Map<Long, KnowledgePoint> kpById = new HashMap<>();
        if (!ids.isEmpty()) {
            for (QuestionKpRow row : questionMapper.selectKpRowsByQuestionIds(ids)) {
                kpIdsByQuestion.computeIfAbsent(row.getQuestionId(), key -> new ArrayList<>())
                        .add(row.getKpId());
            }
            Set<Long> allKpIds = new HashSet<>();
            kpIdsByQuestion.values().forEach(allKpIds::addAll);
            if (!allKpIds.isEmpty()) {
                for (KnowledgePoint kp : knowledgePointMapper.selectByIds(new ArrayList<>(allKpIds))) {
                    kpById.put(kp.getId(), kp);
                }
            }
        }

        List<QuestionVO> list = new ArrayList<>(rows.size());
        for (Question row : rows) {
            list.add(toVO(row, false, kpIdsByQuestion.getOrDefault(row.getId(), List.of()), kpById));
        }
        return new PageVO<>(list, total, query.getPage(), query.getSize());
    }

    @Override
    public QuestionVO detail(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "题目不存在：" + id);
        }
        return toVO(question, true);
    }

    /**
     * 单题补知识点（详情场景）；列表场景走 page() 里的批量查询，避免 N+1。
     */
    private QuestionVO toVO(Question question, boolean withAnswer) {
        List<Long> kpIds = questionMapper.selectKpIds(question.getId());
        Map<Long, KnowledgePoint> kpById = new HashMap<>();
        if (!kpIds.isEmpty()) {
            for (KnowledgePoint kp : knowledgePointMapper.selectByIds(kpIds)) {
                kpById.put(kp.getId(), kp);
            }
        }
        return toVO(question, withAnswer, kpIds, kpById);
    }

    private QuestionVO toVO(Question question, boolean withAnswer,
                            List<Long> kpIds, Map<Long, KnowledgePoint> kpById) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setOptions(question.getOptions());
        vo.setDifficulty(question.getDifficulty());
        vo.setKpIds(kpIds);
        vo.setKpNames(kpIds.stream()
                .map(kpById::get)
                .filter(Objects::nonNull)
                .map(KnowledgePoint::getName)
                .toList());
        if (withAnswer) {
            vo.setAnswer(question.getAnswer());
            vo.setAnalysis(question.getAnalysis());
        }
        return vo;
    }

    private void validateOptions(String options) {
        if (options == null || options.isBlank()) {
            return;
        }
        try {
            var node = objectMapper.readTree(options);
            if (!node.isArray()) {
                throw new IllegalArgumentException("选项必须是 JSON 数组");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("选项不是合法 JSON");
        }
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}