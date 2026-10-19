package com.kaoyan.platform.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.ai.entity.PhotoSearchRecord;
import com.kaoyan.platform.ai.mapper.PhotoSearchRecordMapper;
import com.kaoyan.platform.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhotoHistoryServiceTest {

    @Mock
    private PhotoSearchRecordMapper mapper;

    private PhotoHistoryService service;

    @BeforeEach
    void setUp() {
        service = new PhotoHistoryService(mapper, new ObjectMapper());
    }

    @Test
    @DisplayName("搜题历史：JSON 字段还原为列表")
    void listsRecords() {
        PhotoSearchRecord record = new PhotoSearchRecord();
        record.setId(7L);
        record.setUserId(1L);
        record.setContent("求极限");
        record.setAnswer("1");
        record.setOptionsJson("[\"A. 0\",\"B. 1\"]");
        record.setStepsJson("[\"第一步\"]");
        record.setKnowledgePointsJson("[\"极限\"]");
        record.setFavorite(true);
        record.setCreatedAt(LocalDateTime.now());
        when(mapper.selectPage(1L, null, 0, 10)).thenReturn(List.of(record));
        when(mapper.count(1L, null)).thenReturn(1L);

        var page = service.list(1L, null, 1, 10);

        assertEquals(1, page.getList().size());
        assertEquals(List.of("A. 0", "B. 1"), page.getList().get(0).getOptions());
        assertTrue(page.getList().get(0).isFavorite());
    }

    @Test
    @DisplayName("搜题历史：别人的记录不可见")
    void detailRejectsUnknown() {
        when(mapper.selectByIdAndUser(9L, 1L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.detail(1L, 9L));
    }

    @Test
    @DisplayName("收藏：更新后返回最新状态")
    void updatesFavorite() {
        PhotoSearchRecord record = new PhotoSearchRecord();
        record.setId(7L);
        record.setUserId(1L);
        record.setContent("题");
        record.setFavorite(true);
        when(mapper.updateFavorite(7L, 1L, true)).thenReturn(1);
        when(mapper.selectByIdAndUser(7L, 1L)).thenReturn(record);

        assertTrue(service.favorite(1L, 7L, true).isFavorite());
        verify(mapper).updateFavorite(7L, 1L, true);
    }
}
