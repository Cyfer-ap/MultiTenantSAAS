package com.chacha.multitenantsaas.config;

import com.chacha.multitenantsaas.web.SubscriptionMutationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SubscriptionWebMvcConfiguration implements WebMvcConfigurer {

    private final SubscriptionMutationInterceptor subscriptionMutationInterceptor;

    public SubscriptionWebMvcConfiguration(
            SubscriptionMutationInterceptor subscriptionMutationInterceptor) {
        this.subscriptionMutationInterceptor = subscriptionMutationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(subscriptionMutationInterceptor).addPathPatterns("/api/tenants/**");
    }
}
