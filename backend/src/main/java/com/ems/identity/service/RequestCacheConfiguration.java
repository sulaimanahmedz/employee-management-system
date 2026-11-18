package com.ems.identity.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;

/** API login has no redirect/replay flow; rejected URLs must not be saved in sessions. */
@Configuration
public class RequestCacheConfiguration {
    @Bean
    public RequestCache requestCache() {
        return new NullRequestCache();
    }
}
