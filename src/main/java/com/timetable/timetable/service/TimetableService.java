package com.timetable.timetable.service;

import com.timetable.lecture.dto.CreateCustomLectureRequest;
import com.timetable.lecture.entity.Lecture;
import com.timetable.lecture.entity.UserLecture;
import com.timetable.lecture.repo.LectureRepository;
import com.timetable.lecture.repo.UserLectureRepository;
import com.timetable.timetable.dto.AutoGenerateRequest;
import com.timetable.timetable.dto.TimetableResponse;
import com.timetable.timetable.entity.Timetable;
import com.timetable.timetable.entity.TimetableItem;
import com.timetable.timetable.repo.TimetableItemRepository;
import com.timetable.timetable.repo.TimetableRepository;
import com.timetable.user.entity.User;
import com.timetable.user.repo.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final LectureRepository lectureRepository;
    private final UserLectureRepository userLectureRepository;
    private final TimetableRepository timetableRepository;
    private final TimetableItemRepository timetableItemRepository;
    private final UserRepository userRepository;

    // =========================================================
    // 0. 사용자 커스텀 강의 추가  -> 고정강의(fixed = true)
    // =========================================================
    @Transactional
    public TimetableResponse addCustomLecture(Long userId, CreateCustomLectureRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Timetable timetable = timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(
                        userId, req.getYear(), req.getSemester()
                )
                .orElseGet(() -> {
                    //@Builder.Default 덕분에 items 는 이미 new ArrayList<>() 로 초기화됨
                    Timetable t = Timetable.builder()
                            .user(user)
                            .year(req.getYear())
                            .semester(req.getSemester())
                            .name("사용자 커스텀 시간표")
                            .totalCredits(0)
                            .createdAt(LocalDateTime.now())
                            .build();

                    return timetableRepository.save(t);
                });


        // UserLecture 생성 (사용자 정의 강의)
        UserLecture customLecture = UserLecture.builder()
                .user(user)
                .year(req.getYear())
                .semester(req.getSemester())
                .name(req.getName())
                .credit(req.getCredit())
                .dayPeriod(req.getDayPeriod())
                .code(req.getCode())     // 추가
                .classroom(null)
                .major(false)
                .geCategory(null)
                .majorName(null)
                .memo(null)
                .createdAt(LocalDateTime.now())
                .build();

        userLectureRepository.save(customLecture);

        // timetable_item 생성 -> 고정강의 fixed = true
        List<DayPeriod> slots = parseDayPeriods(req.getDayPeriod());
        for (DayPeriod slot : slots) {
            TimetableItem item = TimetableItem.builder()
                    .lecture(null)
                    .userLecture(customLecture)
                    .day(slot.getDay())
                    .period(slot.getPeriod())
                    .fixed(true)          //고정강의
                    .build();

            //편의 메서드 사용 (양방향 세팅)
            timetable.addItem(item);
            // 내부에서 this.items.add(item); item.setTimetable(this); 둘 다 처리
        }

        timetable.setTotalCredits(computeTotalCreditsFromItems(timetable));
        return buildResponseFromTimetable(timetableRepository.save(timetable));
    }


    // =========================================================
    // 1. 자동 시간표 생성
    //    - 고정강의(fixed = true)는 유지
    //    - 자동 추가된 교양(fixed = false)만 매번 갈아끼우기
    // =========================================================
    @Transactional
    public TimetableResponse autoGenerate(Long userId, AutoGenerateRequest req) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Timetable timetable = timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(
                        userId, req.getYear(), req.getSemester()
                )
                .orElseGet(() -> {
                    Timetable t = Timetable.builder()
                            .user(user)
                            .year(req.getYear())
                            .semester(req.getSemester())
                            .name("자동 생성 시간표")
                            .totalCredits(0)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return timetableRepository.save(t);
                });

        // 2) DB에서 이전 자동생성 아이템(fixed=false) 먼저 bulk delete
        timetableItemRepository.deleteAutoItemsByTimetableId(timetable.getId());

        // 3) 엔티티 컬렉션에서도 fixed=false 아이템 제거 (컬렉션은 '교체'하지 않고 '수정'만!)
        timetable.getItems().removeIf(item -> !item.isFixed());

        // 이제 여기서부터 timetable.getItems() == 고정 강의만 들어 있음
        List<TimetableItem> fixedItems = new ArrayList<>(timetable.getItems());

        // 4) 고정 강의끼리도 겹치면 에러
        if (hasTimeConflictAmongFixedItems(timetable)) {
            throw new IllegalArgumentException("고정 강의끼리 시간이 겹칩니다.");
        }

        // ===== 고정 강의 정보 수집 =====
        Set<String> fixedNames = new HashSet<>();
        int fixedCredits = 0;

        Set<Long> officialIds = new HashSet<>();
        Map<Long, Lecture> fixedOfficialMap = new LinkedHashMap<>();
        Set<Long> customIds = new HashSet<>();

        for (TimetableItem item : fixedItems) {

            if (item.getLecture() != null) {
                Lecture lec = item.getLecture();
                fixedNames.add(lec.getName());

                if (officialIds.add(lec.getId())) {
                    fixedCredits += lec.getCredit();
                    fixedOfficialMap.put(lec.getId(), lec);
                }
            }

            if (item.getUserLecture() != null) {
                UserLecture ul = item.getUserLecture();
                fixedNames.add(ul.getName());

                if (customIds.add(ul.getId())) {
                    fixedCredits += ul.getCredit();
                }
            }
        }

        List<Lecture> fixedOfficialLectures = new ArrayList<>(fixedOfficialMap.values());

        // 5) 고정 슬롯 (요일/교시)
        Set<String> occupiedSlotsByFixed = fixedItems.stream()
                .flatMap(i -> normalizePeriods(i.getPeriod()).stream()
                        .map(p -> slotKey(i.getDay(), p)))
                .collect(Collectors.toSet());

        // 고정 학점만으로도 목표 초과면 그대로 반환
        if (fixedCredits > req.getTargetCredits()) {
            timetable.setName("고정 강의만으로 구성된 시간표");
            timetable.setTotalCredits(fixedCredits);
            return buildResponseFromTimetable(timetableRepository.save(timetable));
        }

        // ===== 공식 강의 후보 & 필터링 =====
        List<Lecture> candidates = lectureRepository
                .findByYearAndSemesterAndGeCategoryIn(
                        req.getYear(), req.getSemester(), req.getGeCategories()
                );

        // 중복 제거 (code + 분반 기준)
        Map<String, Lecture> uniqueMap = new LinkedHashMap<>();
        for (Lecture l : candidates) {
            String key = (l.getCode() == null ? "null" : l.getCode()) + "-" + l.getSection();
            uniqueMap.put(key, l);
        }
        candidates = new ArrayList<>(uniqueMap.values());

        Set<String> freeDays = new HashSet<>(req.getFreeDays());

        List<Lecture> filteredCandidates = candidates.stream()
                .filter(l -> !l.isCustom())                         // 공식 교양만
                .filter(l -> !fixedNames.contains(l.getName()))     // 이미 고정된 과목 이름 제외
                .filter(l -> !isVacationLecture(l))                 // 방학에 열리는 스키, 스노우보드 제외
                .filter(l -> !isOnFreeDay(l, freeDays))             // 자유요일 제외
                .filter(l -> !isConflictWithSlots(l, occupiedSlotsByFixed)) // 고정과 겹치지 않게
                .toList();

        // ===== 랜덤 + 그리디로 최선 스케줄 찾기 =====
        // seed를 조금 바꿔주면, 서버 재시작 후에도 더 랜덤한 느낌
        Random random = new Random(System.nanoTime() ^ userId);

        // 시도 횟수
        int attempts = 40;
        ScheduleCandidate best = null;

        for (int i = 0; i < attempts; i++) {
            ScheduleCandidate candidate = buildRandomSchedule(
                    fixedOfficialLectures,
                    filteredCandidates,
                    occupiedSlotsByFixed,
                    fixedCredits,
                    req.getTargetCredits(),
                    random
            );

            if (best == null) {
                best = candidate;
            } else if (candidate.totalCredits > best.totalCredits) {
                best = candidate;
            } else if (candidate.totalCredits == best.totalCredits && random.nextBoolean()) {
                // 같은 학점이면 가끔 랜덤하게 갈아끼워서 다양성 확보
                best = candidate;
            }
        }


        if (best == null) {
            best = new ScheduleCandidate(fixedOfficialLectures, fixedCredits);
        }

        // ===== 결과를 timetable에 반영 =====
        timetable.setName("자동 생성 시간표");
        timetable.setTotalCredits(best.totalCredits);

        Set<Long> fixedIds = fixedOfficialLectures.stream()
                .map(Lecture::getId)
                .collect(Collectors.toSet());

        List<Lecture> autoLectures = best.lectures.stream()
                .filter(l -> !fixedIds.contains(l.getId()))
                .toList();

        // 6) 이미 차 있는 칸들(고정강의) 기준으로 occupiedSlots 만들기
        Set<String> occupiedSlots = timetable.getItems().stream()
                .flatMap(i -> normalizePeriods(i.getPeriod()).stream()
                        .map(p -> slotKey(i.getDay(), p)))
                .collect(Collectors.toSet());

        // 새로 선택된 교양들(자동 추가)을 timetable_item으로 추가 (fixed=false)
        for (Lecture lec : autoLectures) {

            List<DayPeriod> dps = parseDayPeriods(lec.getDayPeriod());

            // 1) 이 강의가 차지할 모든 정규화된 슬롯을 먼저 계산
            Set<String> lectureSlots = new HashSet<>();
            boolean conflict = false;

            for (DayPeriod dp : dps) {
                for (int norm : normalizePeriods(dp.getPeriod())) {
                    String key = slotKey(dp.getDay(), norm);

                    // 이미 다른 강의가 쓰고 있는 슬롯이면 → 이 강의 전체를 스킵
                    if (occupiedSlots.contains(key)) {
                        conflict = true;
                        break;
                    }
                    lectureSlots.add(key);
                }
                if (conflict) break;
            }

            // 2) 다른 강의들과 충돌하면 이 강의는 통째로 패스
            if (conflict) {
                continue;
            }

            // 3) 충돌이 없다면 → 이제 이 강의의 모든 칸을 실제 시간표에 반영
            //    (occupiedSlots도 여기서 한 번에 업데이트)
            for (DayPeriod dp : dps) {
                for (int norm : normalizePeriods(dp.getPeriod())) {
                    occupiedSlots.add(slotKey(dp.getDay(), norm));
                }

                TimetableItem item = TimetableItem.builder()
                        .lecture(lec)
                        .userLecture(null)
                        .day(dp.getDay())
                        .period(dp.getPeriod())   // DB에는 원래 교시(23,24,25,26 등)를 그대로 저장
                        .fixed(false)
                        .build();

                timetable.addItem(item);
            }
        }


        System.out.println("=== autoGenerate 디버그 ===");
        System.out.println("fixedCredits = " + fixedCredits);
        System.out.println("freeDays = " + req.getFreeDays());
        System.out.println("geCategories = " + req.getGeCategories());
        System.out.println("후보 전체 개수 = " + candidates.size());
        System.out.println("필터 후 후보 개수 = " + filteredCandidates.size());
        System.out.println("best.totalCredits = " + (best != null ? best.totalCredits : -1));

        return buildResponseFromTimetable(timetableRepository.save(timetable));
    }



    // =========================================================
    // 2. 고정강의(사용자 커스텀 강의) 삭제
    //    - 한 번에 그 강의 전체(여러 칸) 삭제하는 버전
    // =========================================================
    @Transactional
    public TimetableResponse deleteCustomFixedLecture(
            Long userId,
            int year,
            int semester,
            Long userLectureId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Timetable timetable = timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(
                        userId, year, semester
                )
                .orElseThrow(() -> new IllegalArgumentException("해당 학기 시간표가 없습니다."));

        UserLecture userLecture = userLectureRepository.findById(userLectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 커스텀 강의입니다."));

        if (!userLecture.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 커스텀 강의만 삭제할 수 있습니다.");
        }

        // 1) timetable_item 에서 이 커스텀 강의에 해당하는 모든 칸 삭제
        timetableItemRepository.deleteByTimetableAndUserLecture(timetable, userLecture);

        // 2) 커스텀 강의 엔티티도 삭제 (원하면 유지해도 됨)
        userLectureRepository.delete(userLecture);

        // 3) 시간표 학점 다시 계산
        Timetable refreshed = timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(
                        userId, year, semester
                )
                .orElseThrow();

        refreshed.setTotalCredits(computeTotalCreditsFromItems(refreshed));
        timetableRepository.save(refreshed);

        return buildResponseFromTimetable(refreshed);
    }

    @Transactional
    public void deleteLecturesByName(Long userId, int year, int semester, String name) {

        // 1) 해당 학기 시간표 찾기
        Timetable timetable = timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(userId, year, semester)
                .orElseThrow(() -> new IllegalArgumentException("해당 학기 시간표가 존재하지 않습니다."));

        if (!timetable.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("이 시간표를 삭제할 권한이 없습니다.");
        }

        // 2) 이름이 같은 강의(공식 or 커스텀)를 가진 칸 모두 찾기
        List<TimetableItem> toRemove = timetable.getItems().stream()
                .filter(item -> {
                    if (item.getLecture() != null &&
                            name.equals(item.getLecture().getName())) {
                        return true;
                    }
                    if (item.getUserLecture() != null &&
                            name.equals(item.getUserLecture().getName())) {
                        return true;
                    }
                    return false;
                })
                .toList();

        // 3) timetable에서 제거 → orphanRemoval = true 덕분에 DB에서도 삭제됨
        for (TimetableItem item : toRemove) {
            timetable.removeItem(item); // items.remove + item.setTimetable(null) 이 메서드
        }
    }


    // =========================================================
    // 헬퍼 함수들
    // =========================================================

    private boolean isConflictWithSlots(Lecture lecture, Set<String> slots) {
        for (String s : extractSlots(lecture))
            if (slots.contains(s)) return true;
        return false;
    }

    private boolean isOnFreeDay(Lecture lec, Set<String> freeDays) {
        if (freeDays.isEmpty()) return false;
        String dp = lec.getDayPeriod();
        if (dp == null || dp.isBlank()) return false;
        return freeDays.contains(dp.substring(0, 1));
    }

    private List<String> extractSlots(Lecture lecture) {
        String dp = lecture.getDayPeriod();
        if (dp == null || dp.isBlank()) return List.of();

        List<String> result = new ArrayList<>();
        for (String c : dp.split("\\s+")) {
            String day = c.substring(0, 1);
            for (String pStr : c.substring(1).split("\\.")) {
                if (pStr.isBlank()) continue;

                int original = Integer.parseInt(pStr);
                for (int norm : normalizePeriods(original)) {
                    result.add(day + "-" + norm);
                }
            }
        }
        return result;
    }


    private boolean hasTimeConflictAmongFixedItems(Timetable timetable) {
        Set<String> slots = new HashSet<>();
        for (TimetableItem i : timetable.getItems()) {
            if (!i.isFixed()) continue;

            for (int p : normalizePeriods(i.getPeriod())) {
                String key = slotKey(i.getDay(), p);
                if (!slots.add(key)) {
                    // 같은 (요일, 정규화된 교시)가 이미 있음 → 시간 충돌
                    return true;
                }
            }
        }
        return false;
    }

    private String slotKey(String day, int period) {
        return day + "-" + period;
    }

    private List<DayPeriod> parseDayPeriods(String dp) {
        List<DayPeriod> result = new ArrayList<>();
        if (dp == null || dp.isBlank()) return result;

        for (String c : dp.split("\\s+")) {
            String day = c.substring(0, 1);
            for (String p : c.substring(1).split("\\.")) {
                result.add(new DayPeriod(day, Integer.parseInt(p)));
            }
        }
        return result;
    }

    private Map<String, Integer> initDayLoadFromSlots(Set<String> slots) {
        Map<String, Integer> map = new HashMap<>();
        for (String s : slots) {
            map.merge(s.substring(0, 1), 1, Integer::sum);
        }
        return map;
    }

    private boolean wouldMakeDayTooHeavy(Map<String, Integer> load, List<String> slots) {
        Map<String, Integer> add = new HashMap<>();
        for (String s : slots) {
            add.merge(s.substring(0, 1), 1, Integer::sum);
        }

        double avg = load.values().stream().mapToInt(i -> i).sum() / 5.0;

        for (String day : add.keySet()) {
            int now = load.getOrDefault(day, 0);
            int newLoad = now + add.get(day);
            if (newLoad > avg + 2) return true;
        }
        return false;
    }

    private int computeTotalCreditsFromItems(Timetable t) {
        Map<String, Integer> map = new HashMap<>();
        for (TimetableItem i : t.getItems()) {
            if (i.getLecture() != null)
                map.putIfAbsent("L-" + i.getLecture().getId(), i.getLecture().getCredit());
            if (i.getUserLecture() != null)
                map.putIfAbsent("U-" + i.getUserLecture().getId(), i.getUserLecture().getCredit());
        }
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }
    private TimetableResponse buildResponseFromTimetable(Timetable timetable) {

        List<TimetableResponse.LectureDto> lectureDtos = timetable.getItems().stream()
                .map(item -> {
                    var lec = item.getLecture();
                    var ul = item.getUserLecture();

                    // 공통 정보 추출
                    Long itemId = item.getId();
                    Long lectureId = (lec != null ? lec.getId() : null);
                    Long userLectureId = (ul != null ? ul.getId() : null);
                    boolean fixed = Boolean.TRUE.equals(item.isFixed());

                    String code;
                    String name;
                    String professor;
                    String dayPeriod;
                    String classroom;
                    String geCategory;
                    int credit;

                    if (lec != null) {
                        //공식 강의
                        code = lec.getCode();
                        name = lec.getName();
                        professor = lec.getProfessor();
                        dayPeriod = lec.getDayPeriod();   // Lecture 엔티티에 이미 문자열로 있을 거라 가정
                        classroom = lec.getClassroom();
                        geCategory = lec.getGeCategory();
                        credit = lec.getCredit();
                    } else if (ul != null) {
                        //사용자 정의 강의
                        code = ul.getCode();
                        name = ul.getName();
                        professor = null;                 // 필요시 ul에 필드 있으면 거기서 꺼내기
                        dayPeriod = ul.getDayPeriod();    // userLecture도 문자열로 가지고 있을 거라 가정
                        classroom = ul.getClassroom();
                        geCategory = ul.getGeCategory();
                        credit = ul.getCredit();
                    } else {
                        // 이 경우는 있으면 안 됨 (디버깅용)
                        code = null;
                        name = "(invalid item)";
                        professor = null;
                        dayPeriod = null;
                        classroom = null;
                        geCategory = null;
                        credit = 0;
                    }

                    return new TimetableResponse.LectureDto(
                            itemId,
                            lectureId,
                            userLectureId,
                            fixed,
                            code,
                            name,
                            professor,
                            dayPeriod,
                            classroom,
                            geCategory,
                            credit
                    );
                })
                .toList();

        return new TimetableResponse(
                timetable.getId(),
                timetable.getYear(),
                timetable.getSemester(),
                timetable.getTotalCredits(),
                lectureDtos
        );
    }


    @Transactional(readOnly = true)
    public TimetableResponse getMyLatest(Long userId, int year, int semester) {
        return timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(userId, year, semester)
                .map(this::buildResponseFromTimetable)
                .orElseGet(() ->
                        // 시간표가 없을 때 프론트가 처리하기 쉬운 "빈 시간표" 응답
                        new TimetableResponse(
                                null,          // timetableId 없음
                                year,
                                semester,
                                0,             // totalCredits = 0
                                List.of()      // lectures = 빈 배열
                        )
                );
    }

    @Transactional
    public void deleteUserLecture(Long userId, Long userLectureId) {
        UserLecture ul = userLectureRepository.findById(userLectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자 정의 강의입니다."));

        // 소유자 체크 (다른 유저 강의 지우는 것 방지)
        if (ul.getUser() == null || !ul.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("이 강의를 삭제할 권한이 없습니다.");
        }

        // 여기서 user_lecture 한 건을 삭제하면
        // timetable_item.user_lecture_id FK에 ON DELETE CASCADE 가 걸려있으면
        // 해당 user_lecture 를 참조하는 timetable_item 들도 DB에서 자동 삭제됨.
        userLectureRepository.delete(ul);
    }

    @Transactional
    public void deleteItem(Long userId, Long itemId) {

        // 1) 삭제할 timetable_item 찾기
        TimetableItem item = timetableItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표 항목입니다. itemId=" + itemId));

        Timetable timetable = item.getTimetable();

        // 2) 소유자 체크
        if (!timetable.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("이 아이템을 삭제할 권한이 없습니다.");
        }

        // 3) 엔티티 연관관계에서 제거 → orphanRemoval = true 덕분에 DB에서도 자동 삭제됨
        timetable.removeItem(item);
    }

    @Transactional
    public void deleteTimetable(Long userId, int year, int semester) {

        // 1) 해당 학기 시간표 조회
        Timetable timetable = timetableRepository
                .findFirstByUserIdAndYearAndSemesterOrderByCreatedAtDesc(userId, year, semester)
                .orElseThrow(() -> new IllegalArgumentException("해당 학기 시간표가 존재하지 않습니다."));

        User user = timetable.getUser();
        if (!user.getId().equals(userId)) {
            throw new IllegalArgumentException("이 시간표를 삭제할 권한이 없습니다.");
        }

        // 2) 이 사용자 + 해당 학기의 user_lecture 목록 미리 조회
        List<UserLecture> customLectures =
                userLectureRepository.findByUserAndYearAndSemester(user, year, semester);

        // 3) 먼저 timetable 삭제
        //    → 연관된 timetable_item 들이 orphanRemoval=true 덕분에 함께 삭제됨
        timetableRepository.delete(timetable);

        // 4) 이제 timetable_item 이 없으므로 user_lecture 를 안전하게 삭제
        if (!customLectures.isEmpty()) {
            userLectureRepository.deleteAll(customLectures);
        }
    }

    // =========================================================
    // 내부 클래스들
    // =========================================================

    @Getter
    @AllArgsConstructor
    static class DayPeriod {
        private String day;
        private int period;
    }


    @RequiredArgsConstructor
    @Getter
    static class ScheduleCandidate {
        final List<Lecture> lectures;  // 고정 + 자동 포함 전체 목록
        final int totalCredits;
    }

    /**
     * 자동완성 랜덤 + 그리디 시도
     */
    /**
     * 랜덤 + 그리디로 하나의 스케줄 후보를 만든다.
     *
     * @param fixedOfficialLectures 이미 고정된(공식) 강의들
     * @param candidates            자동으로 뽑을 수 있는 후보 교양들
     * @param fixedSlots            이미 고정된 (day, period) 슬롯들
     * @param fixedCredits          고정 강의 학점 합
     * @param targetCredits         목표 학점
     * @param random                랜덤 객체
     */
    private ScheduleCandidate buildRandomSchedule(
            List<Lecture> fixedOfficialLectures,
            List<Lecture> candidates,
            Set<String> fixedSlots,
            int fixedCredits,
            int targetCredits,
            Random random
    ) {
        // 현재 상태: 고정 강의 + 이미 찬 슬롯
        List<Lecture> current = new ArrayList<>(fixedOfficialLectures);
        int currentCredits = fixedCredits;
        Set<String> occupied = new HashSet<>(fixedSlots);

        // 후보를 복사해서 섞기 (매 시도마다 순서 랜덤)
        List<Lecture> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool, random);

        for (Lecture lec : pool) {
            int credit = lec.getCredit();

            // 학점 범위 넘어가면 스킵
            if (currentCredits + credit > targetCredits) {
                continue;
            }

            // 슬롯 충돌 체크 (extractSlots 안에서 normalizePeriods 사용)
            if (isConflictWithSlots(lec, occupied)) {
                continue;
            }

            // 이 강의를 선택
            current.add(lec);

            // 이 강의의 모든 (day, period)를 "정규화된 교시" 기준으로 occupied에 반영
            for (DayPeriod dp : parseDayPeriods(lec.getDayPeriod())) {
                for (int norm : normalizePeriods(dp.getPeriod())) {
                    occupied.add(slotKey(dp.getDay(), norm));
                }
            }

            currentCredits += credit;

            if (currentCredits == targetCredits) {
                break;
            }
        }


        return new ScheduleCandidate(current, currentCredits);
    }


    private int compareDayPeriod(String a, String b) {
        if (a == null) return 1;
        if (b == null) return -1;

        String[] days = {"월", "화", "수", "목", "금"};
        Map<String, Integer> order = new HashMap<>();

        for (int i = 0; i < days.length; i++) {
            order.put(days[i], i);
        }

        String dayA = a.substring(0, 1);
        String dayB = b.substring(0, 1);

        int da = order.getOrDefault(dayA, 99);
        int db = order.getOrDefault(dayB, 99);

        if (da != db) return Integer.compare(da, db);

        return Integer.compare(extractFirstPeriod(a), extractFirstPeriod(b));
    }

    private int extractFirstPeriod(String dp) {
        try {
            String rest = dp.substring(1);
            return Integer.parseInt(rest.split("\\.")[0]);
        } catch (Exception e) {
            return 99;
        }
    }
    // 야간 교시(21~26)를 실제 겹치는 주간 교시(1~9)로 정규화
    private List<Integer> normalizePeriods(int period) {
        return switch (period) {
            case 21 -> List.of(1, 2);  // 09:00~10:15  → 1,2교시와 겹침
            case 22 -> List.of(2, 3);  // 10:30~11:45 → 2,3
            case 23 -> List.of(4, 5);  // 12:00~13:15 → 4,5
            case 24 -> List.of(5, 6);  // 13:30~14:45 → 5,6
            case 25 -> List.of(7, 8);  // 15:00~16:15 → 7,8
            case 26 -> List.of(8, 9);  // 16:30~17:45 → 8,9
            default -> List.of(period); // 나머지는 자기 자신
        };
    }

    private boolean isVacationLecture(Lecture lec) {
        String code = lec.getCode();
        String name = lec.getName();

        if (code == null && name == null) return false;

        // 1) 교과코드 기준으로 막기
        if ("GE1987".equals(code) || "GE1321".equals(code)) { // 스키, 스노우보드 코드
            return true;
        }

        // 2) 혹시 코드 바뀌어도 이름으로 방어
        if (name != null &&
                (name.contains("스키") || name.contains("스노우보드"))) {
            return true;
        }

        return false;
    }

}
