package com.timetable.timetable.repo;

import com.timetable.lecture.entity.UserLecture;
import com.timetable.timetable.entity.Timetable;
import com.timetable.timetable.entity.TimetableItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimetableItemRepository extends JpaRepository<TimetableItem, Long> {

    // 1) 이 시간표의 "고정강의"만 조회
    List<TimetableItem> findByTimetableAndFixedTrue(Timetable timetable);

    // 2) 이 시간표의 "자동생성 강의"(fixed=false)를 한 방에 삭제 (bulk delete)
    @Modifying
    @Query("delete from TimetableItem ti where ti.timetable.id = :timetableId and ti.fixed = false")
    void deleteAutoItemsByTimetableId(@Param("timetableId") Long timetableId);

    // 3) 특정 커스텀 강의(UserLecture)에 해당하는 아이템들 삭제 (고정강의 삭제용)
    void deleteByTimetableAndUserLecture(Timetable timetable, UserLecture userLecture);
}
