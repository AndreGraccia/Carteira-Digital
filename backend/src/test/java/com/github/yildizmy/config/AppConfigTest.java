package com.github.yildizmy.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppConfigTest {

    @Test
    void calendarDaysAreSerialisedAsIso8601SoTheClientCanParseThem() throws Exception {
        var builder = new Jackson2ObjectMapperBuilder();
        new AppConfig().jsonCustomizer().customize(builder);

        String json = builder.build().writeValueAsString(Map.of("date", LocalDate.of(2026, 10, 3)));

        assertEquals("{\"date\":\"2026-10-03\"}", json);
    }
}
