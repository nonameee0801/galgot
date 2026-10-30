package com.example.galgot.recommend.controller;

import com.example.galgot.recommend.dto.RecommendationResponse;
import com.example.galgot.recommend.service.RecommendationService;
import com.example.galgot.weather.service.WeatherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * GET /api/recommendations?region=11&date=2026-10-30&type=12&page=0&size=20
 * region(기본 11 서울), date(기본 오늘), type(콘텐츠 유형), page, size 모두 생략 가능. 로그인 없이 조회.
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public RecommendationResponse recommend(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        LocalDate target = date != null ? date : WeatherService.today();
        return recommendationService.recommend(region, target, type, page, size);
    }
}
