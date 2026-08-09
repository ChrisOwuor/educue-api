package com.owuor.educue.common.config;

import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

class TimeConfigTest {
    @Test
    void usesConfiguredBusinessTimezone() {
        TimeConfig config = new TimeConfig();
        ZoneId zone = config.applicationZone("Africa/Nairobi");
        Clock clock = config.clock(zone);
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Africa/Nairobi"));
        assertThat(ZonedDateTime.ofInstant(Instant.parse("2026-01-01T00:00:00Z"), zone).getHour()).isEqualTo(3);
    }
}
