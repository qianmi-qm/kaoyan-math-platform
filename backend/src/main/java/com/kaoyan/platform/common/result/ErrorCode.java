package com.kaoyan.platform.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAM_ERROR(4001, "参数错误"),
    UNAUTHORIZED(4010, "未登录或登录已过期"),
    FORBIDDEN(4030, "无权访问"),
    TOO_MANY_REQUESTS(4290, "请求过于频繁，请稍后再试"),
    NOT_FOUND(5001, "资源不存在"),
    CONFLICT(4090, "资源冲突"),
    SYSTEM_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
