package com.example.galgot.weather.dto;

import com.example.galgot.area.domain.Area;
import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;
import com.example.galgot.weather.domain.WeatherForecast;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** region은 시도 코드, area와 areaName은 날씨를 받은 지역(시도 전체면 "11", "서울 전체"). */
public record WeatherResponse(
        String region,
        String area,
        String areaName,
        LocalDate date,
        Sky sky,
        PrecipType precipType,
        Integer pop,
        Integer tempMax,
        Integer tempMin,
        LocalDateTime fetchedAt
) {
    public static WeatherResponse from(WeatherForecast f, Area area) {
        return new WeatherResponse(f.getLdongRegnCd(), area.getCode(), area.getName(), f.getForecastDate(),
                f.getSky(), f.getPrecipType(), f.getPop(), f.getTempMax(), f.getTempMin(), f.getFetchedAt());
    }
}
