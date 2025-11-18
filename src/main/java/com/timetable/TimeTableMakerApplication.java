// com.timetable.TimeTableMakerApplication
package com.timetable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // ← 이것만 있어도 됨
public class TimeTableMakerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeTableMakerApplication.class, args);
    }
}
