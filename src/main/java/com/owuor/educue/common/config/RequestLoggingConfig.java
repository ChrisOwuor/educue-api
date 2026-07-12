package com.owuor.educue.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();

        filter.setIncludeClientInfo(true);   // IP address
        filter.setIncludeQueryString(true);  // URL params
        filter.setIncludeHeaders(false);     // keep safe
        filter.setIncludePayload(false);     // avoid password leaks
        filter.setMaxPayloadLength(1000);

        filter.setAfterMessagePrefix("REQUEST RECEIVED: ");

        return filter;
    }
}
