package com.owuor.educue.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.time.*;
import java.util.TimeZone;

@Configuration
public class TimeConfig {

    @Bean
    public ZoneId applicationZone(
            @Value("${app.time-zone:Africa/Nairobi}") String zone
    ) {
        ZoneId zoneId = ZoneId.of(zone);
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
        return zoneId;
    }

    @Bean
    public Clock clock(ZoneId applicationZone) {
        return Clock.system(applicationZone);
    }
}
