package com.timetable.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeRequest {
    @NotBlank @Email
    @Pattern(regexp="^[A-Za-z0-9._%+-]+@skuniv\\.ac\\.kr$", message="학교 이메일만 가능합니다.")
    private String email;
}