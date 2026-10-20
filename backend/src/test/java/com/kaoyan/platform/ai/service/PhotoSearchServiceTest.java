package com.kaoyan.platform.ai.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaoyan.platform.ai.config.AiProperties;
import com.kaoyan.platform.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PhotoSearchServiceTest {

    @Mock
    private AiQuotaService quotaService;
    @Mock
    private AiChatClient aiChatClient;

    private PhotoSearchService service;
    private AiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setVisionApiKey("");
        properties.setVisionBaseUrl("https://api.example.com/v1");
        service = new PhotoSearchService(properties, quotaService,
                new ObjectMapper(), aiChatClient);
    }

    @Test
    @DisplayName("拍照搜题：空文件拒绝")
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "q.jpg", "image/jpeg", new byte[0]);
        assertThrows(BusinessException.class, () -> service.search(1L, file));
        verifyNoInteractions(quotaService);
    }

    @Test
    @DisplayName("拍照搜题：非图片拒绝")
    void rejectsNonImage() {
        MockMultipartFile file = new MockMultipartFile("file", "q.txt", "text/plain",
                "not an image".getBytes());
        assertThrows(BusinessException.class, () -> service.search(1L, file));
        verifyNoInteractions(quotaService);
    }

    @Test
    @DisplayName("拍照搜题：未配置视觉 key 时提示，不扣配额")
    void rejectsWhenNoVisionKey() {
        byte[] png = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "q.png", "image/png", png);
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.search(1L, file));
        org.junit.jupiter.api.Assertions.assertTrue(e.getMessage().contains("视觉模型"));
        verifyNoInteractions(quotaService);
    }
}
