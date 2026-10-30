package com.example.galgot.weather.controller;

import com.example.galgot.weather.dto.WeatherResponse;
import com.example.galgot.weather.service.WeatherService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** GET /api/weather?region=11&date=2026-10-30 (둘 다 생략 가능: 서울, 오늘). 로그인 없이 조회. */
@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping
    public WeatherResponse get(
            @RequestParam(name = "region", defaultValue = "11") String region,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : WeatherService.today();
        return WeatherResponse.from(weatherService.getForecast(region, target));
    }
}
