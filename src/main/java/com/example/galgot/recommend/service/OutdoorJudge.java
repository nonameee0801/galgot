package com.example.galgot.recommend.service;

import com.example.galgot.global.config.WeatherRuleProperties;
import com.example.galgot.weather.domain.PrecipType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 그날 야외 활동이 적합한지 판단하는 순수 로직. 하나라도 걸리면 부적합이고, 이유를 함께 돌려준다.
 * 값이 없는(null) 항목은 판단에서 뺀다.
 */
@Component
public class OutdoorJudge {

    public record Result(boolean outdoorSuitable, List<String> reasons) {
    }

    private final WeatherRuleProperties rule;

    public OutdoorJudge(WeatherRuleProperties rule) {
        this.rule = rule;
    }

    public Result judge(PrecipType precipType, Integer pop, Integer tempMax, Integer tempMin) {
        List<String> reasons = new ArrayList<>();

        if (precipType != null && precipType != PrecipType.NONE) {
            reasons.add("강수 예보(" + label(precipType) + ")");
        }
        if (pop != null && pop >= rule.popThreshold()) {
            reasons.add("강수확률 " + pop + "%");
        }
        if (tempMax != null && tempMax >= rule.maxTempThreshold()) {
            reasons.add("최고기온 " + tempMax + "℃");
        }
        if (tempMin != null && tempMin <= rule.minTempThreshold()) {
            reasons.add("최저기온 " + tempMin + "℃");
        }
        return new Result(reasons.isEmpty(), List.copyOf(reasons));
    }

    private static String label(PrecipType type) {
        return switch (type) {
            case RAIN -> "비";
            case RAIN_SNOW -> "비/눈";
            case SNOW -> "눈";
            case SHOWER -> "소나기";
            case NONE -> "없음";
        };
    }
}
