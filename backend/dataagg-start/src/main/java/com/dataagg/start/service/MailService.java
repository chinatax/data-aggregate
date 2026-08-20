package com.dataagg.start.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public void sendVerificationCode(String to, String code) {
        String subject = "数据聚合 - 邮箱验证码";
        String text = "您的验证码是：" + code + "，有效期5分钟。";

        if (mailSender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(to);
                message.setSubject(subject);
                message.setText(text);
                mailSender.send(message);
                log.info("验证码邮件已发送至 {}", to);
                return;
            } catch (Exception e) {
                log.warn("邮件发送失败，降级为控制台输出: {}", e.getMessage());
            }
        }
        log.info("========== 验证码（控制台模式）==========");
        log.info("收件人: {}", to);
        log.info("验证码: {}", code);
        log.info("==========================================");
    }
}
