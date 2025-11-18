package com.timetable.timetable.repo;

import com.timetable.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    List<Timetable> findByUserIdAndYearAndSemester(Long userId, int year, int semester);

    Optional<Timetable> findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(
            Long userId, int year, int semester
    );

}
