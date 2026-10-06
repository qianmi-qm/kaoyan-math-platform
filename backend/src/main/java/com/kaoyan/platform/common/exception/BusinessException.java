package com.kaoyan.platform.common.exception;

import com.kaoyan.platform.common.result.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
