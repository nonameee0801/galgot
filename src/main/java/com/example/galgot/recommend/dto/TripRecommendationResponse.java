package com.example.galgot.recommend.dto;

import com.example.galgot.place.domain.Env;
import com.example.galgot.place.dto.PlaceResponse;
import com.example.galgot.recommend.service.EnvPreference;
import com.example.galgot.weather.dto.WeatherResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 날씨기반 일차별 코스 추천 결과.
 *
 * @param region          시도 코드
 * @param area            고른 지역 코드(서울은 시도 전체라 "11")
 * @param areaName        예: "서울 전체", "가평군"
 * @param start           출발일
 * @param dayCount        일정 길이(1~3)
 * @param env             사용자가 고른 실내/실외/둘 다. null이면 일차마다 그날 추천대로
 * @param startPlaceId    1일차 첫 장소. 화면은 주소에 남겨 새로고침, 공유 때 같은 코스를 다시 만든다
 * @param rerollAvailable 지금 시작 장소 말고 고를 수 있는 시작 장소가 있는지. false면 "다른 추천 받기"를 숨긴다
 * @param days            일차별 날씨, 판정, 방문 순서
 */
public record TripRecommendationResponse(
        String region,
        String area,
        String areaName,
        LocalDate start,
        int dayCount,
        EnvPreference.Choice env,
        Long startPlaceId,
        boolean rerollAvailable,
        List<Day> days
) {

    /**
     * @param recommendedEnv 그날 판정에 따른 추천(OUTDOOR 또는 INDOOR). 일차 탭의 추천 아이콘에 쓴다
     * @param message        그날 판정과 고른 실내/실외에 따른 안내 문장
     * @param notice         장소가 모자라거나 주변 지역에서 채웠을 때의 안내(없으면 null)
     */
    public record Day(
            int dayNo,
            LocalDate date,
            WeatherResponse weather,
            boolean outdoorSuitable,
            Env recommendedEnv,
            List<String> reasons,
            String message,
            List<Stop> places,
            String notice
    ) {
    }

    /**
     * @param order  그날 방문 순서(1부터)
     * @param nearby 고른 지역이 아니라 주변 지역에서 채운 장소
     */
    public record Stop(int order, boolean nearby, PlaceResponse place) {
    }
}
