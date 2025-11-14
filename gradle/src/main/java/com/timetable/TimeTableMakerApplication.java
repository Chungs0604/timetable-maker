// com.timetable.TimeTableMakerApplication
package com.timetable;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.timetable")
@EntityScan(basePackages = "com.timetable.user.entity")
@EnableJpaRepositories(basePackages = "com.timetable.user.repo")
public class TimeTableMakerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimeTableMakerApplication.class, args);
    }
}
