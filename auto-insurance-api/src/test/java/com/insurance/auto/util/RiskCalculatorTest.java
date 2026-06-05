package com.insurance.auto.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskCalculator - Testes Unitários")
class RiskCalculatorTest {

    private RiskCalculator riskCalculator;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @BeforeEach
    void setUp() {
        riskCalculator = new RiskCalculator(FIXED_CLOCK);
    }

    // ========== Multiplicador por Idade do Condutor ==========

    @Test
    @DisplayName("Condutor jovem (< 25 anos) deve ter multiplicador 1.30")
    void youngDriverShouldHaveHigherMultiplier() {
        LocalDate birthDate = LocalDate.of(2004, 6, 1); // 21 anos em 2026
        assertThat(riskCalculator.getAgeRiskMultiplier(birthDate))
                .isEqualByComparingTo(new BigDecimal("1.30"));
    }

    @Test
    @DisplayName("Condutor adulto (25-60 anos) deve ter multiplicador 1.00")
    void adultDriverShouldHaveStandardMultiplier() {
        LocalDate birthDate = LocalDate.of(1990, 3, 10); // 35 anos em 2026
        assertThat(riskCalculator.getAgeRiskMultiplier(birthDate))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Condutor idoso (> 60 anos) deve ter multiplicador 1.15")
    void elderDriverShouldHaveHigherMultiplier() {
        LocalDate birthDate = LocalDate.of(1955, 1, 1); // 71 anos em 2026
        assertThat(riskCalculator.getAgeRiskMultiplier(birthDate))
                .isEqualByComparingTo(new BigDecimal("1.15"));
    }

    // ========== Multiplicador por Experiência ==========

    @Test
    @DisplayName("CNH com menos de 2 anos deve ter multiplicador 1.25")
    void veryNewDriverShouldHaveHighestExperienceMultiplier() {
        LocalDate licenseDate = LocalDate.of(2025, 6, 1); // menos de 1 ano
        assertThat(riskCalculator.getExperienceRiskMultiplier(licenseDate))
                .isEqualByComparingTo(new BigDecimal("1.25"));
    }

    @Test
    @DisplayName("CNH entre 2 e 5 anos deve ter multiplicador 1.10")
    void newDriverShouldHaveModerateExperienceMultiplier() {
        LocalDate licenseDate = LocalDate.of(2023, 1, 1); // 3 anos
        assertThat(riskCalculator.getExperienceRiskMultiplier(licenseDate))
                .isEqualByComparingTo(new BigDecimal("1.10"));
    }

    @Test
    @DisplayName("CNH com 10+ anos deve ter multiplicador 0.95 (desconto)")
    void experiencedDriverShouldHaveDiscount() {
        LocalDate licenseDate = LocalDate.of(2010, 1, 1); // 16 anos
        assertThat(riskCalculator.getExperienceRiskMultiplier(licenseDate))
                .isEqualByComparingTo(new BigDecimal("0.95"));
    }

    // ========== Multiplicador por Histórico de Sinistros ==========

    @Test
    @DisplayName("Sem sinistros deve ter desconto de 10% (0.90)")
    void noClaimsShouldHaveDiscount() {
        assertThat(riskCalculator.getClaimsRiskMultiplier(0))
                .isEqualByComparingTo(new BigDecimal("0.90"));
    }

    @Test
    @DisplayName("1 sinistro deve ter multiplicador 1.10")
    void oneClaimShouldHaveModerateMultiplier() {
        assertThat(riskCalculator.getClaimsRiskMultiplier(1))
                .isEqualByComparingTo(new BigDecimal("1.10"));
    }

    @Test
    @DisplayName("3 sinistros deve ter multiplicador 1.40")
    void threeClaimsShouldHaveHighMultiplier() {
        assertThat(riskCalculator.getClaimsRiskMultiplier(3))
                .isEqualByComparingTo(new BigDecimal("1.40"));
    }

    @Test
    @DisplayName("4+ sinistros deve ter multiplicador máximo 1.60")
    void manyClaimsShouldHaveMaxMultiplier() {
        assertThat(riskCalculator.getClaimsRiskMultiplier(4))
                .isEqualByComparingTo(new BigDecimal("1.60"));
        assertThat(riskCalculator.getClaimsRiskMultiplier(10))
                .isEqualByComparingTo(new BigDecimal("1.60"));
    }

    // ========== Multiplicador por Idade do Veículo ==========

    @Test
    @DisplayName("Veículo novo (até 1 ano) deve ter desconto 0.95")
    void newVehicleShouldHaveDiscount() {
        assertThat(riskCalculator.getVehicleAgeMultiplier(2025))
                .isEqualByComparingTo(new BigDecimal("0.95"));
    }

    @Test
    @DisplayName("Veículo de 2-5 anos deve ter multiplicador padrão 1.00")
    void midAgeVehicleShouldHaveStandardMultiplier() {
        assertThat(riskCalculator.getVehicleAgeMultiplier(2022))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Veículo muito antigo (15+ anos) deve ter multiplicador 1.35")
    void veryOldVehicleShouldHaveHighMultiplier() {
        assertThat(riskCalculator.getVehicleAgeMultiplier(2005))
                .isEqualByComparingTo(new BigDecimal("1.35"));
    }

    // ========== Prêmio Base ==========

    @Test
    @DisplayName("Prêmio base deve ser exatamente 5% do valor do veículo")
    void basePremiumShouldBe5PercentOfVehicleValue() {
        assertThat(riskCalculator.calculateBasePremium(new BigDecimal("100000.00")))
                .isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("Prêmio base deve ser arredondado para 2 casas decimais")
    void basePremiumShouldBeRounded() {
        assertThat(riskCalculator.calculateBasePremium(new BigDecimal("85000.00")))
                .isEqualByComparingTo(new BigDecimal("4250.00"));
    }

    // ========== Multiplicação de Fatores ==========

    @Test
    @DisplayName("Multiplicação de fatores deve retornar produto correto com 4 casas decimais")
    void multiplyFactorsShouldReturnCorrectProduct() {
        BigDecimal result = riskCalculator.multiplyFactors(
                new BigDecimal("1.30"),
                new BigDecimal("0.90"),
                new BigDecimal("1.10")
        );
        assertThat(result).isEqualByComparingTo(new BigDecimal("1.2870"));
    }

    @Test
    @DisplayName("Fator nulo deve ser ignorado na multiplicação")
    void nullFactorShouldBeIgnored() {
        BigDecimal result = riskCalculator.multiplyFactors(
                new BigDecimal("1.30"),
                null,
                new BigDecimal("1.10")
        );
        assertThat(result).isEqualByComparingTo(new BigDecimal("1.4300"));
    }

    // ========== Perfil de Risco ==========

    @Test
    @DisplayName("Multiplicador < 1.20 deve resultar em perfil BAIXO")
    void lowMultiplierShouldResultInLowProfile() {
        assertThat(riskCalculator.getRiskProfile(new BigDecimal("0.95"))).isEqualTo("BAIXO");
        assertThat(riskCalculator.getRiskProfile(new BigDecimal("1.19"))).isEqualTo("BAIXO");
    }

    @Test
    @DisplayName("Multiplicador entre 1.20 e 1.80 deve resultar em perfil MÉDIO")
    void mediumMultiplierShouldResultInMediumProfile() {
        assertThat(riskCalculator.getRiskProfile(new BigDecimal("1.20"))).isEqualTo("MÉDIO");
        assertThat(riskCalculator.getRiskProfile(new BigDecimal("1.50"))).isEqualTo("MÉDIO");
    }

    @Test
    @DisplayName("Multiplicador >= 1.80 deve resultar em perfil ALTO")
    void highMultiplierShouldResultInHighProfile() {
        assertThat(riskCalculator.getRiskProfile(new BigDecimal("1.80"))).isEqualTo("ALTO");
        assertThat(riskCalculator.getRiskProfile(new BigDecimal("2.50"))).isEqualTo("ALTO");
    }
}
