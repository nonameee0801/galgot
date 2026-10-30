package com.example.galgot.course.dto;

import com.example.galgot.course.domain.Course;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 코스 목록용 요약. 담긴 장소는 포함하지 않는다. */
public record CourseResponse(
        Long id,
        String title,
        String ldongRegnCd,
        LocalDate travelDate,
        boolean publicCourse,
        String shareCode,
        LocalDateTime createdAt
) {
    public static CourseResponse from(Course c) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getLdongRegnCd(), c.getTravelDate(),
                c.isPublicCourse(), c.getShareCode(), c.getCreatedAt());
    }
}
