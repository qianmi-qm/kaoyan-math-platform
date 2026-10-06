package com.kaoyan.platform.common.result;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAM_ERROR(4001, "参数错误"),
    NOT_FOUND(5001, "资源不存在"),
    NOT_IMPLEMENTED(5010, "骨架未实现：等待你来完成"),
    SYSTEM_ERROR(9999, "系统内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
