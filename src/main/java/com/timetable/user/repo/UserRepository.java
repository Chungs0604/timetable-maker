// com.timetable.user.repo.UserRepository
package com.timetable.user.repo;

import com.timetable.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByStudentId(String studentId);

    Optional<User> findByStudentId(String studentId); // ⬅ 추가
}
