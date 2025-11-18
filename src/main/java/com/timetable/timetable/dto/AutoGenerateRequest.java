package com.timetable.timetable.dto;

import lombok.Data;

import java.util.List;

@Data
public class AutoGenerateRequest {

    // 년도 (예: 2025)
    private int year;

    // 학기 (예: 1, 2)
    private int semester;

    // 공강 요일 예: ["월", "금"] 또는 ["MON", "FRI"]
    private List<String> freeDays;

    // 교양 카테고리 리스트 예: ["미래와융합", "과학과기술"]
    private List<String> geCategories;

    // 목표 학점 (예: 18)
    private int targetCredits;
}
