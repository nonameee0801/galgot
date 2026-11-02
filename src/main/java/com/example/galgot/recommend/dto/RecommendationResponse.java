package com.example.galgot.recommend.dto;

import com.example.galgot.place.dto.PageResponse;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.place.domain.Env;
import com.example.galgot.weather.dto.WeatherResponse;

import java.util.List;

/**
 * 날씨 기반 추천 결과.
 * env를 고르지 않았으면 그날 추천(recommendedEnv)을 고른 것과 같게 보여 준다.
 * recommendedEnv는 그날 판정에 따른 추천(OUTDOOR 또는 INDOOR)으로, 화면 버튼의 추천 아이콘에 쓴다.
 */
public record RecommendationResponse(
        WeatherResponse weather,
        boolean outdoorSuitable,
        Env recommendedEnv,
        List<String> reasons,
        String message,
        PageResponse<PlaceResponse> places
) {
}
