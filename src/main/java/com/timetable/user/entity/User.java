package com.timetable.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name="idx_users_student_id", columnList = "studentId"),
        @Index(name="idx_users_email", columnList = "email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String studentId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String major;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    // DB 컬럼명이 password라서 이렇게 매핑
    @Column(name = "password", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean emailVerified;

    // @Builder일 때 기본값이 무시되지 않도록 @Builder.Default
    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // 업데이트 시간도 함께 관리 권장
    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 🔧 INSERT/UPDATE 직전에 안전하게 값 보정
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
