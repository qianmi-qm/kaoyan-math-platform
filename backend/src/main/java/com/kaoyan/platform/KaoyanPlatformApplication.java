package com.kaoyan.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KaoyanPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(KaoyanPlatformApplication.class, args);
    }
}
