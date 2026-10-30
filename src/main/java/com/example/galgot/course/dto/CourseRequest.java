package com.example.galgot.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 코스 만들기, 수정 요청. publicCourse를 안 보내면 비공개(false)로 처리한다. */
public record CourseRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 5) String ldongRegnCd,
        LocalDate travelDate,
        Boolean publicCourse
) {
}
