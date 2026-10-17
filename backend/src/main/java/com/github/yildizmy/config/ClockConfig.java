package com.github.yildizmy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.security.SecureRandom;

/**
 * Configuration class for creating a {@link Clock} bean.
 * This bean returns the current instant using the best available system clock in UTC time zone.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }
}
