package com.kaoyan.platform.ai.controller;

import com.kaoyan.platform.ai.dto.PhotoFavoriteRequest;
import com.kaoyan.platform.ai.dto.PhotoSearchSaveRequest;
import com.kaoyan.platform.ai.service.AiExplanationService;
import com.kaoyan.platform.ai.service.AiQuotaService;
import com.kaoyan.platform.ai.service.PhotoHistoryService;
import com.kaoyan.platform.ai.service.PhotoSearchService;
import com.kaoyan.platform.ai.vo.AiQuotaVO;
import com.kaoyan.platform.ai.vo.PhotoSearchRecordVO;
import com.kaoyan.platform.ai.vo.PhotoSearchResult;
import com.kaoyan.platform.common.result.PageVO;
import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.common.support.CurrentUser;
import com.kaoyan.platform.question.vo.QuestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "AI 讲解")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiExplanationService aiExplanationService;
    private final AiQuotaService quotaService;
    private final PhotoSearchService photoSearchService;
    private final PhotoHistoryService photoHistoryService;
    private final CurrentUser currentUser;

    public AiController(AiExplanationService aiExplanationService,
                        AiQuotaService quotaService,
                        PhotoSearchService photoSearchService,
                        PhotoHistoryService photoHistoryService,
                        CurrentUser currentUser) {
        this.aiExplanationService = aiExplanationService;
        this.quotaService = quotaService;
        this.photoSearchService = photoSearchService;
        this.photoHistoryService = photoHistoryService;
        this.currentUser = currentUser;
    }

    @Operation(summary = "错题 AI 讲解：SSE 流式返回，无 key/超时会降级为解析文本")
    @PostMapping(value = "/explain/{recordId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter explain(@PathVariable Long recordId) {
        SseEmitter emitter = new SseEmitter(120_000L);
        aiExplanationService.stream(recordId, emitter);
        return emitter;
    }

    @Operation(summary = "当前用户今日 AI 配额")
    @GetMapping("/quota")
    public Result<AiQuotaVO> quota() {
        return Result.ok(quotaService.quota(currentUser.getUserId()));
    }

    @Operation(summary = "拍照搜题：识别图片并返回答案/步骤")
    @PostMapping(value = "/photo-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<PhotoSearchResult> photoSearch(@RequestPart("file") MultipartFile file) {
        return Result.ok(photoSearchService.search(currentUser.getUserId(), file));
    }

    @Operation(summary = "拍照搜题历史")
    @GetMapping("/photo-history")
    public Result<PageVO<PhotoSearchRecordVO>> photoHistory(            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(photoHistoryService.list(currentUser.getUserId(), favorite, page, size));
    }

    @Operation(summary = "保存拍照搜题结果（用户确认后）")
    @PostMapping("/photo-history")
    public Result<PhotoSearchRecordVO> savePhotoHistory(@RequestBody PhotoSearchSaveRequest request) {
        return Result.ok(photoHistoryService.save(currentUser.getUserId(), request));
    }

    @Operation(summary = "拍照搜题历史详情")
    @GetMapping("/photo-history/{id}")
    public Result<PhotoSearchRecordVO> photoHistoryDetail(@PathVariable Long id) {
        return Result.ok(photoHistoryService.detail(currentUser.getUserId(), id));
    }

    @Operation(summary = "收藏/取消收藏搜题记录")
    @PatchMapping("/photo-history/{id}/favorite")
    public Result<PhotoSearchRecordVO> favoritePhotoHistory(@PathVariable Long id,
                                                             @RequestBody PhotoFavoriteRequest request) {
        return Result.ok(photoHistoryService.favorite(
                currentUser.getUserId(), id, request.isFavorite()));
    }

    @Operation(summary = "拍照搜题同类题")
    @GetMapping("/photo-history/{id}/similar")
    public Result<java.util.List<QuestionVO>> similarPhotoQuestions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") int limit) {
        return Result.ok(photoHistoryService.similar(currentUser.getUserId(), id, limit));
    }

    @Operation(summary = "删除搜题记录")
    @DeleteMapping("/photo-history/{id}")
    public Result<Void> deletePhotoHistory(@PathVariable Long id) {
        photoHistoryService.delete(currentUser.getUserId(), id);
        return Result.ok();
    }
}
