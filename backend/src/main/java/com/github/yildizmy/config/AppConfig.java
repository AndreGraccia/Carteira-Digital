package com.github.yildizmy.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

import static com.github.yildizmy.common.Constants.DATE_TIME_FORMAT;

/**
 * Configuration file used to configure settings of the application.
 */
@Configuration
@EnableScheduling
// Pages go out as {content, page: {size, number, totalElements, totalPages}}, a format Spring keeps stable.
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class AppConfig {

    @Bean
    public JsonMapperBuilderCustomizer jsonCustomizer() {
        // java.time values (LocalDate, Instant…) keep Jackson's ISO 8601 default so clients can parse
        // them unambiguously; only legacy java.util.Date uses the display pattern.
        return builder -> builder.defaultDateFormat(new java.text.SimpleDateFormat(DATE_TIME_FORMAT));
    }
}
