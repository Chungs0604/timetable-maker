package com.timetable.timetable.entity;

import com.timetable.lecture.entity.Lecture;
import com.timetable.lecture.entity.UserLecture;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "timetable_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 시간표에 속한 아이템인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_id", nullable = false)
    private Timetable timetable;

    // 공식 강의 (lecture 테이블)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;   // 이제 nullable=true 로 변경됨

    // 사용자 커스텀 강의 (user_lecture 테이블)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_lecture_id")
    private UserLecture userLecture;

    // 요일: "월", "화", "수" ...
    @Column(nullable = false, length = 5)
    private String day;

    // 교시 번호
    @Column(nullable = false)
    private int period;

    /**
     * 사용자가 직접 입력해서 "고정"시킨 강의인지 여부
     * - true  : 사용자 커스텀 or 고정 강의
     * - false : 자동완성으로 채워진 강의
     */
    @Column(name = "is_fixed", nullable = false)
    private boolean fixed;

    // ---- 편의 메서드 ----

    // 사용자 커스텀 강의인지
    public boolean isCustomLecture() {
        return userLecture != null;
    }

    // 화면 등에 표시할 때 사용하기 좋은 이름
    public String getDisplayName() {
        if (lecture != null) return lecture.getName();
        if (userLecture != null) return userLecture.getName();
        return null;
    }
}
