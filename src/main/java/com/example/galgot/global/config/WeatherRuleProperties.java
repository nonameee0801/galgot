package com.example.galgot.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 야외 활동이 부적합한 날씨의 기준. application.yml의 weather.rule.* 로 바꿀 수 있다.
 * 비어 있으면 기본값(강수확률 60% 이상, 최고 33도 이상, 최저 -10도 이하)을 쓴다.
 */
@ConfigurationProperties(prefix = "weather.rule")
public record WeatherRuleProperties(Integer popThreshold, Integer maxTempThreshold, Integer minTempThreshold) {

    public WeatherRuleProperties {
        popThreshold = popThreshold == null ? 60 : popThreshold;
        maxTempThreshold = maxTempThreshold == null ? 33 : maxTempThreshold;
        minTempThreshold = minTempThreshold == null ? -10 : minTempThreshold;
    }
}
