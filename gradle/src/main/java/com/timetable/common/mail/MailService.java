package com.timetable.common.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    // 우선순위: app.mail.from 있으면 그 값, 없으면 spring.mail.username 사용
    @Value("${app.mail.from:${spring.mail.username}}")
    private String from;

    public void sendVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);          // 발신자(네 학교 이메일 계정)
            message.setTo(toEmail);         // 수신자(사용자가 입력한 이메일)
            message.setSubject("[시간표 메이커] 이메일 인증번호");
            message.setText("안녕하세요!\n\n인증번호: " + code + "\n유효시간: 5분\n");
            mailSender.send(message);

            log.info("[MAIL] sent to={} code={} (유효 5분)", toEmail, code);
        } catch (Exception e) {
            log.error("메일 전송 실패 to={} cause={}", toEmail, e.getMessage(), e);
            throw new IllegalStateException("메일 전송 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
//            log.error("메일 전송 실패 to={}", toEmail, e);
//            throw new IllegalStateException("메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
