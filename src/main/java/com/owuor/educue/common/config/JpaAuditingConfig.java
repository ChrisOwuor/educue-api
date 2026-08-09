package com.owuor.educue.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(
        dateTimeProviderRef = "applicationDateTimeProvider"
)
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider applicationDateTimeProvider(
            Clock clock
    ) {
        return () -> Optional.of(
                LocalDateTime.now(clock)
        );
    }
}
