package org.nikahgator.config;

import org.nikahgator.catalog.ServiceGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ServiceGuard serviceGuard;

    public WebConfig(ServiceGuard serviceGuard) {
        this.serviceGuard = serviceGuard;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(serviceGuard).addPathPatterns("/api/imam/**");
    }
}
