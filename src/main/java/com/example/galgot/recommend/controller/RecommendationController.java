package com.example.galgot.recommend.controller;

import com.example.galgot.area.service.AreaService;
import com.example.galgot.recommend.dto.MarkerResponse;
import com.example.galgot.recommend.dto.RecommendationResponse;
import com.example.galgot.recommend.dto.TripRecommendationResponse;
import com.example.galgot.recommend.service.EnvPreference;
import com.example.galgot.recommend.service.RecommendationService;
import com.example.galgot.recommend.service.TripRecommendationService;
import com.example.galgot.weather.service.WeatherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * GET /api/recommendations?region=41&area=41820&date=2026-11-01&type=12&env=INDOOR&page=0&size=20
 * region(기본 11 서울), area(생략하면 시도 전체), date(기본 오늘), type(콘텐츠 유형),
 * env(INDOOR, OUTDOOR, ALL. 생략하면 그날 추천대로, 잘못된 값이면 400), page, size 모두 생략 가능.
 * keyword(장소 이름 검색어)가 있으면 장소는 서울·경기 전체에서 이름으로만 찾고 area, type, env로 거르지 않는다.
 * 날씨 판정은 region, area 기준 그대로다.
 * 로그인 없이 조회.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final TripRecommendationService tripRecommendationService;
    private final AreaService areaService;

    public RecommendationController(RecommendationService recommendationService,
                                    TripRecommendationService tripRecommendationService,
                                    AreaService areaService) {
        this.recommendationService = recommendationService;
        this.tripRecommendationService = tripRecommendationService;
        this.areaService = areaService;
    }

    @GetMapping
    public RecommendationResponse recommend(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "env", required = false) EnvPreference.Choice env,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        LocalDate target = date != null ? date : WeatherService.today();
        return recommendationService.recommend(areaService.resolve(region, area), target, type, env, keyword, page, size);
    }

    /**
     * GET /api/recommendations/markers?region=41&area=41820&type=12&env=INDOOR&keyword=박물관
     * 지도 마커용. 목록과 같은 조건의 장소(id, 이름, 좌표, 실내/실외)를 페이지 없이 돌려준다. 로그인 없이 조회.
     */
    @GetMapping("/markers")
    public List<MarkerResponse> markers(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "env", required = false) EnvPreference.Choice env,
            @RequestParam(name = "keyword", required = false) String keyword) {
        LocalDate target = date != null ? date : WeatherService.today();
        return recommendationService.markers(areaService.resolve(region, area), target, type, env, keyword);
    }

    /**
     * GET /api/recommendations/trip?region=41&area=41820&start=2026-11-03&days=2&env=INDOOR&type=14
     * 날씨기반 일차별 코스. 일차마다 그날 날씨로 판정하고 하루 4곳(설정)을 방문 순서대로 돌려준다. 로그인 없이 조회.
     * start(기본 오늘), days(1~3, 기본 1), env(생략하면 일차마다 그날 추천대로), type은 생략 가능.
     * 서울은 area 없이, 경기는 area를 꼭 넣는다(아니면 400).
     * startPlaceId: 주소로 다시 들어올 때 같은 코스를 만든다. excludeStartPlaceId: 다른 추천 받기 때 지금 시작 장소를 뺀다.
     */
    @GetMapping("/trip")
    public TripRecommendationResponse trip(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(name = "days", defaultValue = "1") int days,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "env", required = false) EnvPreference.Choice env,
            @RequestParam(name = "startPlaceId", required = false) Long startPlaceId,
            @RequestParam(name = "excludeStartPlaceId", required = false) Long excludeStartPlaceId) {
        LocalDate target = start != null ? start : WeatherService.today();
        return tripRecommendationService.recommend(region, area, target, days, type, env,
                startPlaceId, excludeStartPlaceId);
    }
}
