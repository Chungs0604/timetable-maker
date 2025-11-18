package com.timetable.lecture.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lecture")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연도 / 학기
    private int year;
    private int semester;

    // 구분 (전공/교양/기타 등 CSV에서 가져온 값)
    private String division;   // devide

    @Column(name = "ge_category")
    private String geCategory; // 교양 카테고리 (인문, 사회, 자연 등)

    private String code;       // 과목 코드
    private String name;       // 과목명

    private int section;       // 분반
    private int credit;        // 학점

    private String professor;  // 담당 교수

    @Column(name = "day_period")
    private String dayPeriod;  // 예: "목25.26"

    private String classroom;  // 강의실

    private boolean isMajor;   // 전공 여부
    private String majorName;  // 전공 이름

    /**
     * 사용자가 직접 추가한 강의인지 여부
     * - CSV로부터 import 된 정규 강의: false
     * - 사용자가 CreateCustomLectureRequest로 추가한 강의: true
     */
    @Column(name = "is_custom", nullable = false)
    private boolean custom;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
