package com.example.galgot.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 장소 수집 범위 설정. application.yml의 collect.* 값과 연결된다. */
@ConfigurationProperties(prefix = "collect")
public record CollectProperties(
        boolean runOnStartup,
        List<String> regions,
        List<Integer> contentTypes,
        int pageSize,
        int maxCallsPerRun
) {
    public CollectProperties {
        regions = regions == null || regions.isEmpty() ? List.of("11") : regions;
        contentTypes = contentTypes == null || contentTypes.isEmpty() ? List.of(12, 14, 28) : contentTypes;
        pageSize = pageSize <= 0 ? 100 : pageSize;
        maxCallsPerRun = maxCallsPerRun <= 0 ? 300 : maxCallsPerRun;
    }
}
