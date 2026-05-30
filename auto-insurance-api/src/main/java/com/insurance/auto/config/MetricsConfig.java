package com.insurance.auto.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Métricas e Observabilidade
 * 
 * Métricas Disponíveis:
 * - HTTP: latência, throughput, taxa de erro por endpoint
 * - JVM: uso de memória, threads, garbage collection
 * - Cache: hit rate, miss rate do Redis
 * - DB: connection pool, query time
 * - Custom: cálculos de seguro, consultas ViaCEP
 * 
 * Acesso:
 * - Métricas Prometheus: GET /actuator/prometheus
 * - Health Check: GET /actuator/health
 * - Métricas JSON: GET /actuator/metrics
 * 
 * Integração Grafana:
 * 1. Configurar Prometheus para scrape /actuator/prometheus
 * 2. Importar dashboard Spring Boot no Grafana
 * 3. Visualizar métricas em tempo real
 */
@Configuration
public class MetricsConfig {

    /**
     * Adiciona tags comuns a todas as métricas
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config()
            .commonTags(
                Tags.of(
                    "application", "auto-insurance-api",
                    "environment", "development",
                    "team", "insurance-platform"
                )
            );
    }
}
