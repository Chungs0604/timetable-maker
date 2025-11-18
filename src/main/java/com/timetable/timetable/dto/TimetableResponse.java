package com.timetable.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TimetableResponse {

    private Long timetableId;
    private int year;
    private int semester;
    private int totalCredits;
    private List<LectureDto> lectures;

    @Data
    @AllArgsConstructor
    public static class LectureDto {

        // 프론트에서 삭제 버튼용으로 쓸 필드들
        private Long itemId;        // timetable_item PK (셀 삭제용)
        private Long lectureId;     // 공식 강의면 값 있음
        private Long userLectureId; // 커스텀 강의면 값 있음
        private boolean fixed;      // 고정 강의인지(사용자 입력 or 수동 고정) 여부

        // 기존 필드들
        private String code;
        private String name;
        private String professor;
        private String dayPeriod;
        private String classroom;
        private String geCategory;
        private int credit;
    }
}
