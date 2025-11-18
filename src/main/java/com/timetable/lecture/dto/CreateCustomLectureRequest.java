package com.timetable.lecture.dto;

import lombok.Data;

@Data
public class CreateCustomLectureRequest {
    private int year;
    private int semester;

    private String name;       // 강의명 (필수)
    private String dayPeriod;  // 예: "월10.11", "화23.24"
    private int credit;        // 학점

    private String code;       // 학수번호 (사용자가 입력할 수도 있고 안할 수도 있음)
}
