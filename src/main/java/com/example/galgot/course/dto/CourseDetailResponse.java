package com.example.galgot.course.dto;

import com.example.galgot.course.domain.Course;
import com.example.galgot.course.domain.CoursePlace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** 코스 상세. 담긴 장소가 일차, 방문 순서대로 정렬되어 들어간다. tripDays는 일정(1~3, 장소가 없는 일차도 포함). */
public record CourseDetailResponse(
        Long id,
        String title,
        String ldongRegnCd,
        LocalDate travelDate,
        int tripDays,
        boolean publicCourse,
        String shareCode,
        LocalDateTime createdAt,
        List<CoursePlaceResponse> places
) {
    private static final Comparator<CoursePlace> ORDER = Comparator
            .comparingInt(CoursePlace::getDayNo)
            .thenComparingInt(CoursePlace::getVisitOrder)
            .thenComparing(CoursePlace::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    public static CourseDetailResponse from(Course c) {
        List<CoursePlaceResponse> places = c.getPlaces().stream()
                .sorted(ORDER)
                .map(CoursePlaceResponse::from)
                .toList();
        return new CourseDetailResponse(c.getId(), c.getTitle(), c.getLdongRegnCd(), c.getTravelDate(),
                c.getTripDays(), c.isPublicCourse(), c.getShareCode(), c.getCreatedAt(), places);
    }
}
