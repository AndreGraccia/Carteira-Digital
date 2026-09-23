package com.github.yildizmy.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.yildizmy.common.Constants.DATE_TIME_FORMAT;

/**
 * Configuration file used to configure settings of the application.
 */
@Configuration
public class AppConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        // java.time values (LocalDate, Instant…) keep Jackson's ISO 8601 default so clients can parse
        // them unambiguously; only legacy java.util.Date uses the display pattern.
        return builder -> builder.simpleDateFormat(DATE_TIME_FORMAT);
    }
}
