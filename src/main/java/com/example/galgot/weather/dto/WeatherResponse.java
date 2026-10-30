package com.example.galgot.weather.dto;

import com.example.galgot.weather.domain.PrecipType;
import com.example.galgot.weather.domain.Sky;
import com.example.galgot.weather.domain.WeatherForecast;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeatherResponse(
        String region,
        LocalDate date,
        Sky sky,
        PrecipType precipType,
        Integer pop,
        Integer tempMax,
        Integer tempMin,
        LocalDateTime fetchedAt
) {
    public static WeatherResponse from(WeatherForecast f) {
        return new WeatherResponse(f.getLdongRegnCd(), f.getForecastDate(), f.getSky(), f.getPrecipType(),
                f.getPop(), f.getTempMax(), f.getTempMin(), f.getFetchedAt());
    }
}
