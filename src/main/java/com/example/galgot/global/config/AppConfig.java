package com.example.galgot.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({CollectProperties.class, WeatherRuleProperties.class})
public class AppConfig {
}
