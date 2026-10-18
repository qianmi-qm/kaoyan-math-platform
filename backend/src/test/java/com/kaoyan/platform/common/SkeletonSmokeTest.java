package com.kaoyan.platform.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.kaoyan.platform.common.exception.BusinessException;
import com.kaoyan.platform.common.exception.GlobalExceptionHandler;
import com.kaoyan.platform.common.result.ErrorCode;
import com.kaoyan.platform.common.result.Result;
import com.kaoyan.platform.review.domain.EbbinghausScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 骨架冒烟测试（应当前就全绿）：
 * 证明通用层（Result / 异常处理 / 常量）本身可用。
 * 它是你搭好环境后的第一个检查点：本类绿、另外两个算法测试红 = 骨架状态正确。
 */
@DisplayName("骨架冒烟")
class SkeletonSmokeTest {

    @Test
    @DisplayName("统一响应体：ok 携带数据，code=0")
    void resultOk() {
        Result<String> r = Result.ok("data");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMessage());
        assertEquals("data", r.getData());
    }

    @Test
    @DisplayName("业务异常 → 统一错误结构 + 对应 HTTP 状态")
    void businessExceptionHandled() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.NOT_IMPLEMENTED, "L2 待实现"));
        Result<Void> r = response.getBody();
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
        assertEquals(5010, r.getCode());
        assertEquals("L2 待实现", r.getMessage());
        assertNull(r.getData());
    }

    @Test
    @DisplayName("未知异常 → 500，不向外泄露堆栈")
    void unknownExceptionHandled() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Result<Void>> response = handler.handleUnknown(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(9999, response.getBody().getCode());
    }

    @Test
    @DisplayName("复习周期常量：1/2/4/7/15 共 5 档")
    void intervals() {
        assertEquals(5, EbbinghausScheduler.INTERVALS.length);
        assertEquals(1, EbbinghausScheduler.INTERVALS[0]);
        assertEquals(15, EbbinghausScheduler.INTERVALS[4]);
    }
}
