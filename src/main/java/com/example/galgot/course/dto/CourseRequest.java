package com.example.galgot.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 코스 만들기, 수정 요청. publicCourse를 안 보내면 비공개(false)로 처리한다.
 * tripDays는 일정(1~3). 만들 때 안 보내면 1, 수정할 때 안 보내면 그대로 둔다.
 * placeId, dayNo는 만들 때만 쓴다: 새 코스에 첫 장소를 함께 넣는다(나만의 코스에 넣기, 기획서 18장 D). dayNo를 안 보내면 1일차.
 */
public record CourseRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 5) String ldongRegnCd,
        LocalDate travelDate,
        Boolean publicCourse,
        @Min(1) @Max(3) Integer tripDays,
        Long placeId,
        @Min(1) Integer dayNo
) {
}
