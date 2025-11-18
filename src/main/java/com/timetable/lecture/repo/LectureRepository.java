package com.timetable.lecture.repo;

import com.timetable.lecture.entity.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    List<Lecture> findByYearAndSemester(int year, int semester);

    List<Lecture> findByYearAndSemesterAndGeCategoryIn(
            int year, int semester, List<String> geCategories
    );
}
