package com.kaoyan.platform.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.ai.entity.PhotoSearchRecord;
import com.kaoyan.platform.ai.mapper.PhotoSearchRecordMapper;
import com.kaoyan.platform.ai.vo.PhotoSearchRecordVO;
import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.result.PageVO;
import java.util.List;
import org.springframework.stereotype.Service;

/** 拍照搜题历史：只存识别后的文本，不存原图。 */
@Service
public class PhotoHistoryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PhotoSearchRecordMapper mapper;
    private final ObjectMapper objectMapper;

    public PhotoHistoryService(PhotoSearchRecordMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public PageVO<PhotoSearchRecordVO> list(Long userId, Boolean favorite, int page, int size) {
        int safePage = Math.max(1, page);
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
}
