package com.timetable.user.dto;

public record MeResponse(
        Long id,
        String name,
        String major,
        String studentId,
        String email
) {}