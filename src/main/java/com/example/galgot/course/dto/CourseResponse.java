package com.example.galgot.course.dto;

import com.example.galgot.course.domain.Course;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 코스 목록용 요약. 담긴 장소는 포함하지 않고 장소 수, 일차 수, 일정만 준다(마이페이지 카드).
 * lastDay는 장소가 든 가장 뒤 일차(없으면 0). 나만의 코스에 넣기 팝업이 처음 고를 일차로 쓴다.
 * containsPlace는 목록을 placeId와 함께 불렀을 때만 채운다(나만의 코스에 넣기의 "이미 담김"). 아니면 null.
 */
public record CourseResponse(
        Long id,
        String title,
        String ldongRegnCd,
        LocalDate travelDate,
        boolean publicCourse,
        String shareCode,
        LocalDateTime createdAt,
        int placeCount,
        int dayCount,
        int tripDays,
        int lastDay,
        Boolean containsPlace
) {
    public static CourseResponse from(Course c) {
        return from(c, null);
    }

    public static CourseResponse from(Course c, Long placeId) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getLdongRegnCd(), c.getTravelDate(),
                c.isPublicCourse(), c.getShareCode(), c.getCreatedAt(), c.getPlaces().size(), c.dayCount(),
                c.getTripDays(), c.lastUsedDay(), placeId == null ? null : c.containsPlace(placeId));
    }
}
