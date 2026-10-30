package com.example.galgot.envrule.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * classify.run-on-startup=true 일 때만 앱 시작 시 한 번 분류를 실행한다.
 * 끝나면 CLASSIFY_RUN_ON_STARTUP 환경 변수를 지워 두자.
 */
@Component
@ConditionalOnProperty(name = "classify.run-on-startup", havingValue = "true")
public class EnvClassifyRunner implements ApplicationRunner {

    private final EnvClassifyService classifyService;

    public EnvClassifyRunner(EnvClassifyService classifyService) {
        this.classifyService = classifyService;
    }

    @Override
    public void run(ApplicationArguments args) {
        classifyService.reclassifyAll();
    }
}
