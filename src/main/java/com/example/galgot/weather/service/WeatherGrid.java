package com.example.galgot.weather.service;

import java.util.Map;
import java.util.Optional;

/**
 * 시도(법정동 코드) → 기상청 격자(nx, ny) 대표 지점.
 * 서울 60,127(실제 호출로 확인). 경기는 수원 기준 60,121로 둔 값이라 필요하면 조정한다.
 */
public final class WeatherGrid {

    public record Grid(int nx, int ny) {
    }

    private static final Map<String, Grid> GRIDS = Map.of(
            "11", new Grid(60, 127),
            "41", new Grid(60, 121));

    private WeatherGrid() {
    }

    public static Optional<Grid> of(String region) {
        return Optional.ofNullable(region == null ? null : GRIDS.get(region.trim()));
    }
}
