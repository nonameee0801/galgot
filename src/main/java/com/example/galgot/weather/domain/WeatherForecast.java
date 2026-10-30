package com.example.galgot.weather.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 지역(시도) + 날짜별 하루 요약 예보. (ldong_regn_cd, forecast_date)가 유일하다. */
@Entity
@Table(name = "weather_forecasts")
public class WeatherForecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ldong_regn_cd", nullable = false, length = 5)
    private String ldongRegnCd;

    @Column(name = "nx", nullable = false)
    private int nx;

    @Column(name = "ny", nullable = false)
    private int ny;

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sky", length = 20)
    private Sky sky;

    @Enumerated(EnumType.STRING)
    @Column(name = "precip_type", length = 20)
    private PrecipType precipType;

    @Column(name = "pop")
    private Integer pop;

    @Column(name = "temp_max")
    private Integer tempMax;

    @Column(name = "temp_min")
    private Integer tempMin;

    @Column(name = "pm_grade", length = 20)
    private String pmGrade;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    protected WeatherForecast() {
    }

    public static WeatherForecast create(String ldongRegnCd, int nx, int ny, LocalDate date) {
        WeatherForecast f = new WeatherForecast();
        f.ldongRegnCd = ldongRegnCd;
        f.nx = nx;
        f.ny = ny;
        f.forecastDate = date;
        return f;
    }

    /** 새로 받은 하루 요약으로 값을 덮어쓴다. */
    public void apply(Sky sky, PrecipType precipType, Integer pop, Integer tempMax, Integer tempMin,
                      LocalDateTime fetchedAt) {
        this.sky = sky;
        this.precipType = precipType;
        this.pop = pop;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() { return id; }
    public String getLdongRegnCd() { return ldongRegnCd; }
    public int getNx() { return nx; }
    public int getNy() { return ny; }
    public LocalDate getForecastDate() { return forecastDate; }
    public Sky getSky() { return sky; }
    public PrecipType getPrecipType() { return precipType; }
    public Integer getPop() { return pop; }
    public Integer getTempMax() { return tempMax; }
    public Integer getTempMin() { return tempMin; }
    public String getPmGrade() { return pmGrade; }
    public LocalDateTime getFetchedAt() { return fetchedAt; }
}
