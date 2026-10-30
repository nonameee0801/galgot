package com.example.galgot.course.dto;

import com.example.galgot.course.domain.CoursePlace;
import com.example.galgot.place.dto.PlaceResponse;

public record CoursePlaceResponse(
        Long id,
        int dayNo,
        int visitOrder,
        String memo,
        PlaceResponse place
) {
    public static CoursePlaceResponse from(CoursePlace cp) {
        return new CoursePlaceResponse(cp.getId(), cp.getDayNo(), cp.getVisitOrder(), cp.getMemo(),
                PlaceResponse.from(cp.getPlace()));
    }
}
