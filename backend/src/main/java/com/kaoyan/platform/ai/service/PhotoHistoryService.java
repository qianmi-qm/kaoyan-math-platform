package com.kaoyan.platform.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.ai.dto.PhotoSearchSaveRequest;
import com.kaoyan.platform.ai.entity.PhotoSearchRecord;
import com.kaoyan.platform.ai.mapper.PhotoSearchRecordMapper;
import com.kaoyan.platform.ai.vo.PhotoSearchRecordVO;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.result.PageVO;
import com.kaoyan.platform.question.service.QuestionService;
import com.kaoyan.platform.question.vo.QuestionVO;
import java.util.List;
import org.springframework.stereotype.Service;

/** 拍照搜题历史：只存识别后的文本，不存原图。 */
@Service
public class PhotoHistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PhotoSearchRecordMapper mapper;
    private final QuestionService questionService;
    private final ObjectMapper objectMapper;

    public PhotoHistoryService(PhotoSearchRecordMapper mapper, QuestionService questionService,
                               ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.questionService = questionService;
        this.objectMapper = objectMapper;
    }

    public PageVO<PhotoSearchRecordVO> list(Long userId, Boolean favorite, int page, int size) {        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        List<PhotoSearchRecordVO> list = mapper.selectPage(userId, favorite,
                        (safePage - 1) * safeSize, safeSize)
                .stream().map(this::toVO).toList();
        return new PageVO<>(list, mapper.count(userId, favorite), safePage, safeSize);
    }

    public PhotoSearchRecordVO detail(Long userId, Long id) {
        return toVO(require(userId, id));
    }

    public void delete(Long userId, Long id) {
        if (mapper.delete(id, userId) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搜题记录不存在");
        }
    }

    public PhotoSearchRecordVO favorite(Long userId, Long id, boolean favorite) {
        if (mapper.updateFavorite(id, userId, favorite) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搜题记录不存在");
        }
        return toVO(require(userId, id));
    }

    /** 用户确认后再落库，避免识别错误直接污染错题本。 */
    public PhotoSearchRecordVO save(Long userId, PhotoSearchSaveRequest request) {
        if (request == null || request.getContent() == null || request.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "识别结果为空，不能保存");
        }
        PhotoSearchRecord record = new PhotoSearchRecord();
        record.setUserId(userId);
        record.setContent(request.getContent());
        record.setOptionsJson(write(request.getOptions()));
        record.setAnswer(request.getAnswer());
        record.setStepsJson(write(request.getSteps()));
        record.setKnowledgePointsJson(write(request.getKnowledgePoints()));
        record.setConfidence(request.getConfidence());
        record.setWarningsJson(write(request.getWarnings()));
        record.setModel(request.getModel());
        record.setFavorite(request.isFavorite());
        mapper.insert(record);
        return toVO(require(userId, record.getId()));
    }

    public List<QuestionVO> similar(Long userId, Long id, int limit) {
        PhotoSearchRecord record = require(userId, id);
        return questionService.similar(readList(record.getKnowledgePointsJson()), limit);
    }

    private PhotoSearchRecord require(Long userId, Long id) {
        PhotoSearchRecord record = mapper.selectByIdAndUser(id, userId);
        if (record == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "搜题记录不存在");
        }
        return record;
    }

    private PhotoSearchRecordVO toVO(PhotoSearchRecord record) {
        return PhotoSearchRecordVO.builder()
                .id(record.getId())
                .content(record.getContent())
                .options(readList(record.getOptionsJson()))
                .answer(record.getAnswer())
                .steps(readList(record.getStepsJson()))
                .knowledgePoints(readList(record.getKnowledgePointsJson()))
                .confidence(record.getConfidence())
                .warnings(readList(record.getWarningsJson()))
                .model(record.getModel())
                .favorite(Boolean.TRUE.equals(record.getFavorite()))
                .createdAt(record.getCreatedAt())
                .build();
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return List.of();
        }
    }

    private String write(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception e) {
            return "[]";
        }
    }
}
