package com.insurance.auto.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração Web MVC para registrar interceptors
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/**") // Aplica rate limit em todos os endpoints da API
            .excludePathPatterns(
                "/actuator/**",  // Exclui endpoints de monitoramento
                "/swagger-ui/**", // Exclui Swagger UI
                "/api-docs/**"   // Exclui documentação OpenAPI
            );
    }
}
