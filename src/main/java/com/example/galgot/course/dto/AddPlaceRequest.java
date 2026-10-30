package com.example.galgot.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 코스에 장소 담기. placeId는 /api/places 응답의 id, dayNo를 안 보내면 1일차. */
public record AddPlaceRequest(
        @NotNull Long placeId,
        @Min(1) Integer dayNo,
        @Size(max = 500) String memo
) {
}
