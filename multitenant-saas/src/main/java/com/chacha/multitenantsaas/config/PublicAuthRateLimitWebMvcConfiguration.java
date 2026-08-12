package com.chacha.multitenantsaas.config;

import com.chacha.multitenantsaas.web.PublicAuthRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PublicAuthRateLimitWebMvcConfiguration implements WebMvcConfigurer {

    private final PublicAuthRateLimitInterceptor publicAuthRateLimitInterceptor;

    public PublicAuthRateLimitWebMvcConfiguration(
            PublicAuthRateLimitInterceptor publicAuthRateLimitInterceptor) {
        this.publicAuthRateLimitInterceptor = publicAuthRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(publicAuthRateLimitInterceptor).addPathPatterns("/api/**");
    }
}
