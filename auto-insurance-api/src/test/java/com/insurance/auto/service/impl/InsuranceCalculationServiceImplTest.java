package com.insurance.auto.service.impl;

import com.insurance.auto.client.ViaCepClient;
import com.insurance.auto.dto.request.CalculationRequestDTO;
import com.insurance.auto.dto.response.CalculationResponseDTO;
import com.insurance.auto.dto.viacep.ViaCepResponse;
import com.insurance.auto.enums.*;
import com.insurance.auto.exception.ViaCepException;
import com.insurance.auto.util.RiskCalculator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceCalculationServiceImpl - Testes Unitários")
class InsuranceCalculationServiceImplTest {

    @Mock
    private ViaCepClient viaCepClient;

    private InsuranceCalculationServiceImpl service;

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-01-01T00:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @BeforeEach
    void setUp() {
        RiskCalculator riskCalculator = new RiskCalculator(FIXED_CLOCK);
        service = new InsuranceCalculationServiceImpl(
                viaCepClient, riskCalculator, new SimpleMeterRegistry()
        );
    }

    @Test
    @DisplayName("Deve calcular prêmio com sucesso quando ViaCEP retorna estado SP")
    void shouldCalculatePremiumSuccessfullyWithViaCep() {
        ViaCepResponse viaCepResponse = new ViaCepResponse();
        viaCepResponse.setState("SP");
        when(viaCepClient.getAddressByZipCode("01310100")).thenReturn(viaCepResponse);

        CalculationResponseDTO response = service.calculateInsurancePremium(buildStandardRequest());

        assertThat(response).isNotNull();
        assertThat(response.getQuoteId()).isNotBlank();
        assertThat(response.getTotalPremium()).isPositive();
        assertThat(response.getMonthlyPremium()).isPositive();
        assertThat(response.getDriverLocation()).isEqualTo("São Paulo");
        assertThat(response.getRiskFactors()).containsKey("Localização Geográfica");
        assertThat(response.getRiskFactors().get("Localização Geográfica"))
                .isEqualByComparingTo(new BigDecimal("1.25"));
    }

    @Test
    @DisplayName("Deve usar multiplicador padrão (1.0) quando ViaCEP lança exceção")
    void shouldUseDefaultMultiplierWhenViaCepFails() {
        when(viaCepClient.getAddressByZipCode(anyString()))
                .thenThrow(new ViaCepException("Serviço indisponível"));

        CalculationResponseDTO response = service.calculateInsurancePremium(buildStandardRequest());

        assertThat(response).isNotNull();
        assertThat(response.getDriverLocation()).isEqualTo("Localização não identificada");
        assertThat(response.getTotalPremium()).isPositive();
        assertThat(response.getRiskFactors().get("Localização Geográfica"))
                .isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("Condutor de alto risco deve ter prêmio maior que condutor padrão")
    void highRiskDriverShouldHaveHigherPremiumThanStandard() {
        ViaCepResponse viaCepResponse = new ViaCepResponse();
        viaCepResponse.setState("SP");
        when(viaCepClient.getAddressByZipCode(anyString())).thenReturn(viaCepResponse);

        CalculationResponseDTO highRisk = service.calculateInsurancePremium(buildHighRiskRequest());
        CalculationResponseDTO standard = service.calculateInsurancePremium(buildStandardRequest());

        assertThat(highRisk.getTotalPremium()).isGreaterThan(standard.getTotalPremium());
        assertThat(highRisk.getRiskProfile()).isEqualTo("ALTO");
    }

    @Test
    @DisplayName("Veículo elétrico deve ter prêmio menor que veículo a gasolina")
    void electricVehicleShouldHaveLowerPremiumThanGasoline() {
        ViaCepResponse viaCepResponse = new ViaCepResponse();
        viaCepResponse.setState("MG");
        when(viaCepClient.getAddressByZipCode(anyString())).thenReturn(viaCepResponse);

        CalculationRequestDTO electricRequest = buildStandardRequest();
        electricRequest.setVehicleFuelType(FuelType.ELECTRIC);

        CalculationRequestDTO gasolineRequest = buildStandardRequest();
        gasolineRequest.setVehicleFuelType(FuelType.GASOLINE);

        BigDecimal electricPremium = service.calculateInsurancePremium(electricRequest).getTotalPremium();
        BigDecimal gasolinePremium = service.calculateInsurancePremium(gasolineRequest).getTotalPremium();

        assertThat(electricPremium).isLessThan(gasolinePremium);
    }

    @Test
    @DisplayName("Motocicleta deve ter prêmio maior que sedan")
    void motorcycleShouldHaveHigherPremiumThanSedan() {
        ViaCepResponse viaCepResponse = new ViaCepResponse();
        viaCepResponse.setState("GO");
        when(viaCepClient.getAddressByZipCode(anyString())).thenReturn(viaCepResponse);

        CalculationRequestDTO motorcycleRequest = buildStandardRequest();
        motorcycleRequest.setVehicleCategory(VehicleCategory.MOTORCYCLE);

        CalculationRequestDTO sedanRequest = buildStandardRequest();
        sedanRequest.setVehicleCategory(VehicleCategory.SEDAN);

        BigDecimal motorcyclePremium = service.calculateInsurancePremium(motorcycleRequest).getTotalPremium();
        BigDecimal sedanPremium = service.calculateInsurancePremium(sedanRequest).getTotalPremium();

        assertThat(motorcyclePremium).isGreaterThan(sedanPremium);
    }

    @Test
    @DisplayName("Resposta deve conter todos os fatores de risco esperados")
    void responseShouldContainAllExpectedRiskFactors() {
        when(viaCepClient.getAddressByZipCode(anyString()))
                .thenThrow(new ViaCepException("fallback"));

        CalculationResponseDTO response = service.calculateInsurancePremium(buildStandardRequest());

        assertThat(response.getRiskFactors()).containsKeys(
                "Idade do Condutor",
                "Experiência ao Volante",
                "Histórico de Sinistros",
                "Categoria do Veículo",
                "Tipo de Combustível",
                "Condição do Veículo",
                "Idade do Veículo",
                "Localização Geográfica"
        );
    }

    @Test
    @DisplayName("Prêmio mensal deve ser o prêmio total dividido por 12")
    void monthlyPremiumShouldBeTotalDividedBy12() {
        when(viaCepClient.getAddressByZipCode(anyString()))
                .thenThrow(new ViaCepException("fallback"));

        CalculationResponseDTO response = service.calculateInsurancePremium(buildStandardRequest());

        BigDecimal expectedMonthly = response.getTotalPremium()
                .divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);

        assertThat(response.getMonthlyPremium()).isEqualByComparingTo(expectedMonthly);
    }

    // ========== Fixtures ==========

    private CalculationRequestDTO buildStandardRequest() {
        CalculationRequestDTO request = new CalculationRequestDTO();
        request.setDriverName("João Silva");
        request.setDriverCpf("52998224725");
        request.setDriverBirthDate(LocalDate.of(1985, 3, 15));
        request.setDriverGender(Gender.MALE);
        request.setDriverZipCode("01310100");
        request.setDriverLicenseIssueDate(LocalDate.of(2005, 6, 20));
        request.setDriverEmail("joao.silva@email.com");
        request.setDriverPhone("11987654321");
        request.setDriverClaimsHistory(0);
        request.setVehicleBrand("Toyota");
        request.setVehicleModel("Corolla");
        request.setVehicleManufacturingYear(2022);
        request.setVehicleModelYear(2023);
        request.setVehicleFuelType(FuelType.FLEX);
        request.setVehicleCategory(VehicleCategory.SEDAN);
        request.setVehicleCondition(VehicleCondition.GOOD);
        request.setVehicleMarketValue(new BigDecimal("85000.00"));
        request.setVehicleLicensePlate("ABC1D23");
        return request;
    }

    private CalculationRequestDTO buildHighRiskRequest() {
        CalculationRequestDTO request = new CalculationRequestDTO();
        request.setDriverName("Pedro Jovem");
        request.setDriverCpf("52998224725");
        request.setDriverBirthDate(LocalDate.of(2005, 1, 1)); // 20 anos - jovem
        request.setDriverGender(Gender.MALE);
        request.setDriverZipCode("20000100");
        request.setDriverLicenseIssueDate(LocalDate.of(2024, 6, 1)); // < 2 anos de CNH
        request.setDriverClaimsHistory(4);
        request.setVehicleBrand("Honda");
        request.setVehicleModel("CBR 600");
        request.setVehicleManufacturingYear(2000);
        request.setVehicleModelYear(2001);
        request.setVehicleFuelType(FuelType.GASOLINE);
        request.setVehicleCategory(VehicleCategory.MOTORCYCLE);
        request.setVehicleCondition(VehicleCondition.POOR);
        request.setVehicleMarketValue(new BigDecimal("50000.00"));
        return request;
    }
}
