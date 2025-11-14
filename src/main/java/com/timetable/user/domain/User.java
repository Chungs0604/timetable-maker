//package com.timetable.user.domain;
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.LocalDateTime;
//
//@Table(name="users",
//        indexes = {
//                @Index(name="idx_user_email", columnList = "email", unique = true),
//                @Index(name="idx_user_student_id", columnList = "studentId", unique = true)
//        }
//)
//@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
//public class User {
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable=false, length=100) private String name;
//    @Column(nullable=false, length=100) private String major;
//    @Column(nullable=false, length=32, unique=true) private String studentId;
//    @Column(nullable=false, length=255) private String passwordHash;
//    @Column(nullable=false, length=120, unique=true) private String email;
//    @Column(nullable=false) private boolean emailVerified;
//
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//
//    @PrePersist void onCreate(){ createdAt=LocalDateTime.now(); updatedAt=createdAt; }
//    @PreUpdate void onUpdate(){ updatedAt=LocalDateTime.now(); }
//}