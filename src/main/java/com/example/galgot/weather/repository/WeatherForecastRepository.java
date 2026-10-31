package com.example.galgot.weather.repository;

import com.example.galgot.weather.domain.WeatherForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WeatherForecastRepository extends JpaRepository<WeatherForecast, Long> {

    Optional<WeatherForecast> findByAreaCodeAndForecastDate(String areaCode, LocalDate forecastDate);

    List<WeatherForecast> findAllByAreaCodeAndForecastDateIn(String areaCode, Collection<LocalDate> dates);
}
