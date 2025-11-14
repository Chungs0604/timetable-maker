package com.timetable.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String major;
    @NotBlank private String name;

    @NotBlank
    @Pattern(regexp="^[0-9]{8,12}$", message="학번 형식이 올바르지 않습니다.")
    private String studentId;

    @NotBlank @Size(min=8, max=64)
    private String password;

    @NotBlank @Email
    @Pattern(regexp="^[A-Za-z0-9._%+-]+@skuniv\\.ac\\.kr$", message="학교 이메일만 가능합니다.")
    private String email;

    @NotBlank
    @Pattern(regexp="^[0-9]{6}$", message="인증번호는 6자리 숫자")
    private String code;
}