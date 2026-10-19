package com.kaoyan.platform.auth.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** 邮件发送；未配置 SMTP 时把链接打到日志，方便本地验收。 */
@Slf4j
@Service
public class AuthMailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;
    private final String baseUrl;
    private final boolean logLinks;

    public AuthMailService(ObjectProvider<JavaMailSender> mailSender,
                           @Value("${app.mail.from:no-reply@kaoyan.local}") String from,
                           @Value("${app.mail.base-url:http://localhost:5173}") String baseUrl,
                           @Value("${app.mail.log-links:true}") boolean logLinks) {
        this.mailSender = mailSender;
        this.from = from;
        this.baseUrl = baseUrl;
        this.logLinks = logLinks;
    }

    public void sendReset(String email, String token) {
        send(email, "重置密码", baseUrl + "/reset-password?token=" + encode(token));
    }

    public void sendVerify(String email, String token) {
        send(email, "验证邮箱", baseUrl + "/verify-email?token=" + encode(token));
    }

    private void send(String email, String subject, String link) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(email);
                message.setSubject(subject);
                message.setText("请打开链接完成操作：" + link);
                sender.send(message);
                return;
            } catch (Exception e) {
                log.warn("发送邮件失败，降级为日志：{}", e.getMessage());
            }
        }
        if (logLinks) {
            log.info("[mail:{}] {} -> {}", subject, email, link);
        }
    }

    private String encode(String token) {
        return URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
