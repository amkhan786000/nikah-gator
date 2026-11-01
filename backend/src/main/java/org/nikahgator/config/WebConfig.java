package org.nikahgator.config;

import org.nikahgator.audit.SensitiveViewLogger;
import org.nikahgator.catalog.ServiceGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ServiceGuard serviceGuard;
    private final SensitiveViewLogger sensitiveViews;

    public WebConfig(ServiceGuard serviceGuard, SensitiveViewLogger sensitiveViews) {
        this.serviceGuard = serviceGuard;
        this.sensitiveViews = sensitiveViews;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(serviceGuard).addPathPatterns("/api/imam/**");
        // access log: opening a counseling, shahada or divorce record
        registry.addInterceptor(sensitiveViews).addPathPatterns("/api/imam/svc/**");
    }
}
