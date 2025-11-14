// com.timetable.auth.dto.LoginRequest
package com.timetable.auth.dto;
import jakarta.validation.constraints.NotBlank;

    public record LoginRequest(
        @NotBlank String studentId,
        @NotBlank String password
) {}