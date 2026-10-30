package com.example.galgot.weather.service;

import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;

import java.time.LocalDate;

/** 시간별 예보를 하루 단위로 줄인 값. 값이 없으면 null. */
public record DailySummary(LocalDate date, Sky sky, PrecipType precipType,
                           Integer pop, Integer tempMax, Integer tempMin) {
}
