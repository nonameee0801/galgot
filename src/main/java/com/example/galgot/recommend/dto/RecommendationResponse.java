package com.example.galgot.recommend.dto;

import com.example.galgot.place.dto.PageResponse;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.weather.dto.WeatherResponse;

import java.util.List;

/**
 * 날씨 기반 추천 결과.
 * outdoorSuitable=false 인 날은 야외 장소를 빼고 실내 장소를 먼저 보여 준다.
 */
public record RecommendationResponse(
        WeatherResponse weather,
        boolean outdoorSuitable,
        List<String> reasons,
        String message,
        PageResponse<PlaceResponse> places
) {
}
