package com.example.galgot.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 일차별 코스 추천 설정. application.yml의 recommend.course.* 로 바꿀 수 있다.
 * 하루에 추천할 장소 수(placesPerDay)는 사용자가 고르지 않고 이 설정으로만 바꾼다. 비어 있으면 4곳.
 */
@ConfigurationProperties(prefix = "recommend.course")
public record RecommendCourseProperties(Integer placesPerDay) {

    public RecommendCourseProperties {
        placesPerDay = placesPerDay == null ? 4 : placesPerDay;
        if (placesPerDay < 1) {
            throw new IllegalArgumentException("recommend.course.places-per-day는 1 이상이어야 합니다: " + placesPerDay);
        }
    }
}
