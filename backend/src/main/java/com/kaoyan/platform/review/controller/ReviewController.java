package com.kaoyan.platform.review.controller;

import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.review.service.ReviewService;
import com.kaoyan.platform.review.vo.ReviewStatsVO;
import com.kaoyan.platform.review.vo.ReviewTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "艾宾浩斯复习")
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "今日到期复习清单（含逾期顺延）")
    @GetMapping("/today")
    public Result<List<ReviewTaskVO>> todayList() {
        return Result.ok(reviewService.todayList());
    }

    @Operation(summary = "提交复习结果：remembered=true 记住了 / false 忘了")
    @PostMapping("/{reviewId}/result")
    public Result<ReviewTaskVO> submitResult(@PathVariable Long reviewId,
                                             @RequestParam boolean remembered) {
        return Result.ok(reviewService.submitResult(reviewId, remembered));
    }

    @Operation(summary = "复习队列概览：总数 / 今日到期 / 各周期积压")
    @GetMapping("/stats")
    public Result<ReviewStatsVO> stats() {
        return Result.ok(reviewService.stats());
    }
}
