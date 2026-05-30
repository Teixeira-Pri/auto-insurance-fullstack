package com.insurance.auto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuração do Clock para gerenciamento de tempo
 * 
 * Benefícios:
 * - Permite injetar Clock.systemDefaultZone() em produção
 * - Permite injetar Clock.fixed() em testes
 * - Facilita testes determinísticos de lógica temporal
 * - Segue boas práticas de testabilidade (Dependency Injection)
 * 
 * Uso em Testes:
 * @TestConfiguration
 * class TestClockConfig {
 *     @Bean
 *     public Clock clock() {
 *         return Clock.fixed(
 *             Instant.parse("2026-05-22T10:00:00Z"),
 *             ZoneId.systemDefault()
 *         );
 *     }
 * }
 */
@Configuration
public class ClockConfig {

    /**
     * Fornece o Clock do sistema para uso em produção
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
