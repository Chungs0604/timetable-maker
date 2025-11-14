package com.timetable.user.service;


import com.timetable.user.entity.User;
import com.timetable.user.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public void ensureNotDuplicated(String email, String studentId){
        if (userRepository.existsByEmail(email)) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (userRepository.existsByStudentId(studentId)) throw new IllegalArgumentException("이미 사용 중인 학번입니다.");
    }

    public User create(String name, String major, String studentId, String rawPassword, String email){
        User u = User.builder()
                .name(name)
                .major(major)
                .studentId(studentId)
                .passwordHash(encoder.encode(rawPassword))
                .email(email)
                .emailVerified(true)
                .build();
        return userRepository.save(u);
    }
}