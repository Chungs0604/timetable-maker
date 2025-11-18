package com.timetable.timetable.controller;

import com.timetable.lecture.dto.CreateCustomLectureRequest;
import com.timetable.timetable.dto.AutoGenerateRequest;
import com.timetable.timetable.dto.TimetableResponse;
import com.timetable.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timetables")
public class TimetableController {


    private final TimetableService timetableService;

    /**
     * 1) 사용자 커스텀 강의 추가
     *    POST /api/timetables/{year}/{semester}/custom-lectures
     *    Authorization: Bearer <토큰>
     */
    @PostMapping("/{year}/{semester}/custom-lectures")
    public ResponseEntity<TimetableResponse> addCustomLecture(
            Authentication authentication,
            @PathVariable int year,
            @PathVariable int semester,
            @RequestBody CreateCustomLectureRequest request
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) authentication.getPrincipal();

        // year/semester는 path에서 강제 적용
        request.setYear(year);
        request.setSemester(semester);

        TimetableResponse resp = timetableService.addCustomLecture(userId, request);
        return ResponseEntity.ok(resp);
    }

    /**
     * 2) 자동 시간표 생성
     *    POST /api/timetables/auto-generate
     *    Authorization: Bearer <토큰>
     */
    @PostMapping("/auto-generate")
    public ResponseEntity<TimetableResponse> autoGenerate(
            Authentication authentication,
            @RequestBody AutoGenerateRequest request
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) authentication.getPrincipal();
        TimetableResponse resp = timetableService.autoGenerate(userId, request);
        return ResponseEntity.ok(resp);
    }

    /**
     * 3) 내 시간표 조회 (해당 학기의 최신 버전)
     *    GET /api/timetables/my?year=2025&semester=1
     */
    @GetMapping("/my")
    public ResponseEntity<TimetableResponse> getMyTimetable(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int semester
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) authentication.getPrincipal();

        TimetableResponse resp = timetableService.getMyLatest(userId, year, semester);
        return ResponseEntity.ok(resp);
    }


    /**
     * 4) 사용자 정의 강의(user_lecture) 삭제
     *    DELETE /api/timetables/user-lectures/{userLectureId}
     */
    @DeleteMapping("/user-lectures/{userLectureId}")
    public ResponseEntity<Void> deleteUserLecture(
            Authentication authentication,
            @PathVariable Long userLectureId
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = (Long) authentication.getPrincipal();
        timetableService.deleteUserLecture(userId, userLectureId);
        return ResponseEntity.noContent().build(); // 204
    }

    /**
     * 5) 특정 시간표 아이템(강의 한 칸) 삭제 (원하면 이거도 같이 사용)
     *    DELETE /api/timetables/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteTimetableItem(
            Authentication authentication,
            @PathVariable Long itemId
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = (Long) authentication.getPrincipal();
        timetableService.deleteItem(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 6) 특정 학기 시간표 전체 삭제
     *    DELETE /api/timetables/{year}/{semester}
     */
    @DeleteMapping("/{year}/{semester}")
    public ResponseEntity<Void> deleteTimetable(
            Authentication authentication,
            @PathVariable int year,
            @PathVariable int semester
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = (Long) authentication.getPrincipal();
        timetableService.deleteTimetable(userId, year, semester);
        return ResponseEntity.noContent().build();
    }
    /**
     * 7) 특정 학기에서 같은 이름의 강의 모두 삭제
     *    - 공식 강의, 사용자 정의 강의 둘 다 대상
     *    DELETE /api/timetables/{year}/{semester}/lectures/by-name?name=동양고전의이해
     */
    @DeleteMapping("/{year}/{semester}/lectures/by-name")
    public ResponseEntity<Void> deleteLecturesByName(
            Authentication authentication,
            @PathVariable int year,
            @PathVariable int semester,
            @RequestParam String name
    ) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = (Long) authentication.getPrincipal();
        timetableService.deleteLecturesByName(userId, year, semester, name);
        return ResponseEntity.noContent().build();
    }

}

