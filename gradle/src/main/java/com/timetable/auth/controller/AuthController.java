package com.timetable.auth.controller;

import com.timetable.auth.dto.RegisterRequest;
import com.timetable.auth.dto.SendCodeRequest;
import com.timetable.auth.service.VerificationCodeService;
import com.timetable.common.mail.MailService;
import com.timetable.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/auth") @RequiredArgsConstructor
public class AuthController {
    private final VerificationCodeService codeService;
    private final MailService mailService;
    private final UserService userService;

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody @Valid SendCodeRequest req) {
        // 간단 쿨다운 60초
        if (codeService.inCooldown(req.getEmail())) {
            return ResponseEntity.badRequest().body("잠시 후 다시 시도해주세요.");
        }
        // 코드 발급(5분)
        String code = codeService.generate6Digit();
        codeService.saveCode(req.getEmail(), code, 300);
        codeService.putCooldown(req.getEmail(), 60);
        mailService.sendVerificationCode(req.getEmail(), code);
        return ResponseEntity.ok("인증번호가 발송되었습니다.(개발용: 콘솔 로그 확인)");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        userService.ensureNotDuplicated(req.getEmail(), req.getStudentId());
        boolean ok = codeService.verify(req.getEmail(), req.getCode());
        if (!ok) return ResponseEntity.badRequest().body("인증번호가 올바르지 않거나 만료되었습니다.");
        userService.create(req.getName(), req.getMajor(), req.getStudentId(), req.getPassword(), req.getEmail());
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }
}