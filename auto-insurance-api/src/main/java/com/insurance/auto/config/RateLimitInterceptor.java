package com.insurance.auto.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interceptor de Rate Limiting usando Bucket4j
 * 
 * Implementa o algoritmo Token Bucket para limitar requisições por IP.
 * 
 * Benefícios:
 * - Previne abuso da API (DDoS, scrapers)
 * - Protege recursos computacionais (CPU, memória, DB connections)
 * - Garante fair usage entre clientes
 * - Controla custos de infraestrutura
 * 
 * Algoritmo Token Bucket:
 * - Cada IP tem um "balde" com tokens
 * - Cada requisição consome 1 token
 * - Tokens são reabastecidos a uma taxa fixa (100/min)
 * - Requisição sem tokens disponíveis = 429 Too Many Requests
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${rate.limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${rate.limit.enabled:true}")
    private boolean rateLimitEnabled;

    // Cache em memória dos buckets por IP
    // Em produção, considere usar Redis para compartilhar entre instâncias
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
            HttpServletRequest request, 
            HttpServletResponse response, 
            Object handler) throws Exception {

        // Permite desabilitar rate limit em ambientes de desenvolvimento
        if (!rateLimitEnabled) {
            return true;
        }

        String clientIp = getClientIp(request);
        Bucket bucket = resolveBucket(clientIp);

        // Tenta consumir 1 token do bucket
        if (bucket.tryConsume(1)) {
            // Adiciona headers informativos
            long remainingTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));
            response.setHeader("X-Rate-Limit-Limit", String.valueOf(requestsPerMinute));
            
            log.debug("Rate limit OK for IP: {} | Remaining: {}", clientIp, remainingTokens);
            return true;
        }

        // Limite excedido - retorna 429 Too Many Requests
        log.warn("Rate limit exceeded for IP: {}", clientIp);
        
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"error\":\"Too Many Requests\",\"message\":\"Limite de %d requisições por minuto excedido. Tente novamente em instantes.\"}",
            requestsPerMinute
        ));
        
        return false;
    }

    /**
     * Resolve ou cria um bucket para o IP especificado
     */
    private Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, k -> createNewBucket());
    }

    /**
     * Cria um novo bucket com limite configurável
     * 
     * Estratégia:
     * - Capacidade: requestsPerMinute tokens
     * - Refill: requestsPerMinute tokens a cada minuto
     * - Greedy refill: tokens são adicionados gradualmente (não em lote)
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            requestsPerMinute,
            Refill.greedy(requestsPerMinute, Duration.ofMinutes(1))
        );
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Extrai o IP real do cliente, considerando proxies/load balancers
     */
    private String getClientIp(HttpServletRequest request) {
        // Ordem de prioridade para detectar IP real
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For pode conter múltiplos IPs separados por vírgula
                return ip.split(",")[0].trim();
            }
        }

        // Fallback para IP direto da conexão
        return request.getRemoteAddr();
    }
}
