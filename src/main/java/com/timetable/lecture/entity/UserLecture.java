package com.timetable.lecture.entity;

import com.timetable.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_lecture",
        indexes = {
                @Index(name = "idx_user_lecture_user_semester", columnList = "user_id, year, semester")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자가 만든 강의이므로 User FK 필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer semester;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Integer credit;

    @Column(name = "day_period", length = 50)
    private String dayPeriod;

    @Column(length = 100)
    private String classroom;

    @Column(name = "is_major", nullable = false)
    private boolean major;

    @Column(name = "ge_category", length = 100)
    private String geCategory;

    @Column(name = "major_name", length = 100)
    private String majorName;

    private String memo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private String code;
}
