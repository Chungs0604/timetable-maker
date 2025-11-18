package com.timetable.lecture.repo;

import com.timetable.lecture.entity.UserLecture;
import com.timetable.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLectureRepository extends JpaRepository<UserLecture, Long> {

    List<UserLecture> findByUserIdAndYearAndSemester(Long userId, Integer year, Integer semester);
    List<UserLecture> findByUserAndYearAndSemester(User user, int year, int semester);
}
