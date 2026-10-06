package com.kaoyan.platform.common.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import lombok.Getter;

@Component
public class CurrentUser {

    @Getter
    @Value("${app.default-user-id}")
    private Long userId;
}
