package com.example.galgot.place.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * collect.run-on-startup=true 일 때만 앱 시작 시 한 번 수집을 실행한다.
 * 수집이 끝나면 COLLECT_RUN_ON_STARTUP 값을 지우거나 false로 되돌려 두자.
 */
@Component
@ConditionalOnProperty(name = "collect.run-on-startup", havingValue = "true")
public class PlaceCollectRunner implements ApplicationRunner {

    private final PlaceCollectService collectService;

    public PlaceCollectRunner(PlaceCollectService collectService) {
        this.collectService = collectService;
    }

    @Override
    public void run(ApplicationArguments args) {
        collectService.collect();
    }
}
