package com.timetable.user.controller;

import com.timetable.user.dto.MeResponse;
import com.timetable.user.entity.User;
import com.timetable.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;


    /**
     * 🔹 [GET] /api/me
     * 현재 로그인한 사용자 정보 조회 API
     *
     * 🔸 설명:
     *  - JWT 인증을 통과한 사용자의 정보를 반환.
     *  - Authorization 헤더에 accessToken이 있어야 사용 가능.
     *
     * 🔸 요청 헤더:
     *  Authorization: Bearer <JWT_ACCESS_TOKEN>
     *
     * 🔸 응답(JSON):
     *  {
     *    "id": 1,
     *    "name": "홍길동",
     *    "major": "컴퓨터공학과",
     *    "studentId": "202312345",
     *    "email": "user@skuniv.ac.kr"
     *  }
     *
     * 🔸 실패(토큰 없음 또는 잘못된 토큰):
     *  401 Unauthorized
     */
    @GetMapping("/api/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Long userId = (Long) authentication.getPrincipal();
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) return ResponseEntity.status(401).body("Unauthorized");

        return ResponseEntity.ok(new MeResponse(
                u.getId(), u.getName(), u.getMajor(), u.getStudentId(), u.getEmail()
        ));
    }
}
