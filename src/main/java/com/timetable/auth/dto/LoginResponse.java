// com.timetable.auth.dto.LoginResponse
package com.timetable.auth.dto;

public record LoginResponse(
        String accessToken,
        long   expiresInSeconds
) {}