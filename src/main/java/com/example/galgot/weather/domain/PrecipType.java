package com.example.galgot.weather.domain;

/**
 * 강수 형태(PTY). 선언 순서가 곧 심한 정도(뒤로 갈수록 야외 활동에 나쁨)라서,
 * 하루 중 가장 나쁜 값을 고를 때 ordinal 비교를 쓴다.
 */
public enum PrecipType {
    NONE, SHOWER, RAIN, RAIN_SNOW, SNOW;

    /** 기상청 코드: 0 없음, 1 비, 2 비/눈, 3 눈, 4 소나기 (5~7은 초단기 코드지만 안전하게 같이 처리). */
    public static PrecipType fromCode(String code) {
        return switch (code == null ? "" : code.trim()) {
            case "0" -> NONE;
            case "1", "5" -> RAIN;
            case "2", "6" -> RAIN_SNOW;
            case "3", "7" -> SNOW;
            case "4" -> SHOWER;
            default -> null;
        };
    }
}
