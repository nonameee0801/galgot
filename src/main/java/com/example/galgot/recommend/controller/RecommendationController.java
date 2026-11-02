package com.example.galgot.recommend.controller;

import com.example.galgot.area.service.AreaService;
import com.example.galgot.recommend.dto.MarkerResponse;
import com.example.galgot.recommend.dto.RecommendationResponse;
import com.example.galgot.recommend.service.EnvPreference;
import com.example.galgot.recommend.service.RecommendationService;
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
 * 로그인 없이 조회.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final AreaService areaService;

    public RecommendationController(RecommendationService recommendationService, AreaService areaService) {
        this.recommendationService = recommendationService;
        this.areaService = areaService;
    }

    @GetMapping
    public RecommendationResponse recommend(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "env", required = false) EnvPreference.Choice env,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        LocalDate target = date != null ? date : WeatherService.today();
        return recommendationService.recommend(areaService.resolve(region, area), target, type, env, page, size);
    }

    /**
     * GET /api/recommendations/markers?region=41&area=41820&type=12&env=INDOOR
     * 지도 마커용. 목록과 같은 조건의 장소(id, 이름, 좌표, 실내/실외)를 페이지 없이 돌려준다. 로그인 없이 조회.
     */
    @GetMapping("/markers")
    public List<MarkerResponse> markers(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "env", required = false) EnvPreference.Choice env) {
        LocalDate target = date != null ? date : WeatherService.today();
        return recommendationService.markers(areaService.resolve(region, area), target, type, env);
    }
}
