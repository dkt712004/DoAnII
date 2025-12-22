package com.dkt.userservice.config;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicFilterConfig {
    private final ApplicationContext applicationContext;


    public void configureFilter(HttpSecurity httpSecurity) {
        List<Filter> filters = applicationContext.getBeansOfType(Filter.class).values().stream().toList();

        for (Filter filter:filters) {
            log.info("filter: {}", filter.getClass());
            httpSecurity.addFilterAfter(filter, BasicAuthenticationFilter.class);
        }
    }
}
