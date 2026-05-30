package com.insurance.auto.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

/**
 * Calculadora de Fatores de Risco - Versão Testável
 * 
 * Refatoração Enterprise:
 * - Injeção de Clock para datas determináveis em testes
 * - Component Spring para facilitar mocking
 * - Logs de auditoria dos cálculos
 * - Métodos thread-safe (stateless)
 * 
 * Benefícios:
 * - Testes unitários determinísticos (não dependem da data atual)
 * - Facilita testes de regressão
 * - Permite simular cenários futuros/passados
 * - Alinhado com boas práticas de Clean Code
 */
@Component
@RequiredArgsConstructor
public class RiskCalculator {

    private final Clock clock;
    
    private static final BigDecimal BASE_PREMIUM_RATE = new BigDecimal("0.05"); // 5% do valor do veículo

    /**
     * Calcula o prêmio base (5% do valor do veículo)
     */
    public BigDecimal calculateBasePremium(BigDecimal vehicleValue) {
        return vehicleValue
            .multiply(BASE_PREMIUM_RATE)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula multiplicador de risco por idade do condutor
     * 
     * Regras:
     * - < 25 anos: +30% (jovens = maior risco)
     * - 25-60 anos: padrão (faixa de menor risco)
     * - > 60 anos: +15% (reflexos reduzidos)
     */
    public BigDecimal getAgeRiskMultiplier(LocalDate birthDate) {
        LocalDate today = LocalDate.now(clock); // Usa clock injetável
        int age = Period.between(birthDate, today).getYears();

        if (age < 25) {
            return new BigDecimal("1.30"); // +30% para jovens
        } else if (age >= 25 && age <= 60) {
            return BigDecimal.ONE;         // Faixa padrão
        } else {
            return new BigDecimal("1.15"); // +15% para idosos
        }
    }

    /**
     * Calcula multiplicador por tempo de habilitação
     * 
     * Regras:
     * - < 2 anos: +25% (motorista muito novo)
     * - 2-5 anos: +10% (pouca experiência)
     * - 5-10 anos: padrão
     * - 10+ anos: -5% (desconto para experientes)
     */
    public BigDecimal getExperienceRiskMultiplier(LocalDate licenseIssueDate) {
        LocalDate today = LocalDate.now(clock);
        int experienceYears = Period.between(licenseIssueDate, today).getYears();

        if (experienceYears < 2) {
            return new BigDecimal("1.25"); // Motorista muito novo
        } else if (experienceYears < 5) {
            return new BigDecimal("1.10"); // Pouca experiência
        } else if (experienceYears >= 10) {
            return new BigDecimal("0.95"); // Desconto para experientes
        } else {
            return BigDecimal.ONE;
        }
    }

    /**
     * Calcula multiplicador por histórico de sinistros
     * 
     * Cada sinistro aumenta significativamente o risco.
     * Sem sinistros = desconto de 10% (bônus)
     */
    public BigDecimal getClaimsRiskMultiplier(Integer claimsCount) {
        if (claimsCount == 0) {
            return new BigDecimal("0.90"); // Desconto: sem sinistros
        } else if (claimsCount == 1) {
            return new BigDecimal("1.10");
        } else if (claimsCount == 2) {
            return new BigDecimal("1.25");
        } else if (claimsCount == 3) {
            return new BigDecimal("1.40");
        } else {
            return new BigDecimal("1.60"); // Alto risco: 4+ sinistros
        }
    }

    /**
     * Calcula multiplicador por idade do veículo
     * 
     * Veículos mais antigos têm maior risco de falhas mecânicas
     */
    public BigDecimal getVehicleAgeMultiplier(Integer manufacturingYear) {
        LocalDate today = LocalDate.now(clock);
        int vehicleAge = today.getYear() - manufacturingYear;

        if (vehicleAge <= 1) {
            return new BigDecimal("0.95"); // Novo: desconto
        } else if (vehicleAge <= 5) {
            return BigDecimal.ONE;
        } else if (vehicleAge <= 10) {
            return new BigDecimal("1.10");
        } else if (vehicleAge <= 15) {
            return new BigDecimal("1.20");
        } else {
            return new BigDecimal("1.35"); // Muito antigo
        }
    }

    /**
     * Multiplica todos os fatores de risco
     */
    public BigDecimal multiplyFactors(BigDecimal... factors) {
        BigDecimal result = BigDecimal.ONE;
        for (BigDecimal factor : factors) {
            if (factor != null) {
                result = result.multiply(factor);
            }
        }
        return result.setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Determina o perfil de risco baseado no multiplicador final
     * 
     * Classificação:
     * - BAIXO: < 1.20 (até 20% acima do prêmio base)
     * - MÉDIO: 1.20 - 1.80
     * - ALTO: > 1.80
     */
    public String getRiskProfile(BigDecimal totalMultiplier) {
        if (totalMultiplier.compareTo(new BigDecimal("1.20")) < 0) {
            return "BAIXO";
        } else if (totalMultiplier.compareTo(new BigDecimal("1.80")) < 0) {
            return "MÉDIO";
        } else {
            return "ALTO";
        }
    }
}
