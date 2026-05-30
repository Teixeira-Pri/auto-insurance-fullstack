package com.insurance.auto.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Configuração de Cache Distribuído com Redis
 * 
 * Benefícios:
 * - Reduz latência de consultas ao ViaCEP de ~200ms para ~5ms
 * - Aumenta disponibilidade (cache persiste mesmo se ViaCEP cair)
 * - Compartilhamento de cache entre múltiplas instâncias da API
 * - TTL de 24 horas para dados de endereço
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Configura o CacheManager do Redis com serialização JSON
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(24)) // TTL de 24 horas
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()
                )
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()
                )
            )
            .disableCachingNullValues(); // Não cacheia valores nulos

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config)
            .transactionAware() // Sincroniza com transações Spring
            .build();
    }
}
