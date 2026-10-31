package com.example.galgot.weather.controller;

import com.example.galgot.area.domain.Area;
import com.example.galgot.area.service.AreaService;
import com.example.galgot.weather.dto.WeatherResponse;
import com.example.galgot.weather.service.WeatherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * GET /api/weather?region=41&area=41820&date=2026-11-01
 * region(기본 11 서울), area(생략하면 시도 전체), date(기본 오늘). 로그인 없이 조회.
 */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;
    private final AreaService areaService;

    public WeatherController(WeatherService weatherService, AreaService areaService) {
        this.weatherService = weatherService;
        this.areaService = areaService;
    }

    @GetMapping
    public WeatherResponse get(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "area", required = false) String area,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Area target = areaService.resolve(region, area);
        LocalDate day = date != null ? date : WeatherService.today();
        return WeatherResponse.from(weatherService.getForecast(target, day), target);
    }
}
