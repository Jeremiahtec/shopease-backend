package com.shopease.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AppPropertiesConfig {

    @Bean
    public AppProperties appProperties(Environment environment) {
        return AppProperties.from(environment);
    }
}
