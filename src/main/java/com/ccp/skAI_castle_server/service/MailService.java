package com.ccp.skAI_castle_server.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendReviewReminder(String toEmail, int reviewCount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("[skAI Castle] 오늘의 복습 알림");
            message.setText("오늘 복습할 카드가 " + reviewCount + "개 있습니다.\n" +
                    "지금 바로 복습을 시작해 보세요!");
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Failed to send review reminder email to {}: {}", toEmail, e.getMessage());
        }
    }
}
