package com.timetable.auth.controller;

import com.timetable.auth.dto.LoginRequest;
import com.timetable.auth.dto.LoginResponse;
import com.timetable.auth.dto.RegisterRequest;
import com.timetable.auth.dto.SendCodeRequest;
import com.timetable.auth.service.VerificationCodeService;
import com.timetable.common.jwt.JwtService;
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
    private final JwtService jwtService;

    /**
     * [POST] /auth/send-code
     * 이메일로 인증번호를 발송하는 API
     * - 요청: { "email": "사용자 이메일" }
     * - 응답: "인증번호가 발송되었습니다."
     */
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

    /**
     * [POST] /auth/register
     * 회원가입 API
     * - 이메일 인증번호 검증 후 사용자 생성
     * - 요청: RegisterRequest(name, studentId, major, password, email, code)
     * - 응답: "회원가입이 완료되었습니다."
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest req) {
        userService.ensureNotDuplicated(req.getEmail(), req.getStudentId());
        boolean ok = codeService.verify(req.getEmail(), req.getCode());
        if (!ok) return ResponseEntity.badRequest().body("인증번호가 올바르지 않거나 만료되었습니다.");
        userService.create(req.getName(), req.getMajor(), req.getStudentId(), req.getPassword(), req.getEmail());
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }
    /**
     * [POST] /auth/login
     * 로그인 + JWT 발급 API
     * - 요청: { "studentId": "학번", "password": "비밀번호" }
     * - 응답: { "accessToken": "...", "expiresInSeconds": 3600 }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest req) {
        var user = userService.authenticateByStudentId(req.studentId(), req.password());
        var token = jwtService.issue(user.getId(), user.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, jwtService.getExpiresSeconds()));
    }
}