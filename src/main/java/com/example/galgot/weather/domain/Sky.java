package com.example.galgot.weather.domain;

/** 하늘 상태(SKY). 기상청 코드: 1 맑음, 3 구름많음, 4 흐림. */
public enum Sky {
    CLEAR, MOSTLY_CLOUDY, OVERCAST;

    public static Sky fromCode(String code) {
        return switch (code == null ? "" : code.trim()) {
            case "1" -> CLEAR;
            case "3" -> MOSTLY_CLOUDY;
            case "4" -> OVERCAST;
            default -> null;
        };
    }
}
