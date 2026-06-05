package com.insurance.auto.service.impl;

import com.insurance.auto.client.ViaCepClient;
import com.insurance.auto.dto.request.CalculationRequestDTO;
import com.insurance.auto.dto.response.CalculationResponseDTO;
import com.insurance.auto.dto.viacep.ViaCepResponse;
import com.insurance.auto.enums.BrazilianState;
import com.insurance.auto.exception.ViaCepException;
import com.insurance.auto.service.InsuranceCalculationService;
import com.insurance.auto.util.RiskCalculator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementação Enterprise do Service de Cálculo de Seguro
 * 
 * Evoluções:
 * - Métricas customizadas com Micrometer
 * - Trace IDs automáticos nos logs
 * - RiskCalculator injetável (testável)
 * - Cache Redis no ViaCepClient
 * - Rate limiting no Controller
 */
@Service
@Slf4j
public class InsuranceCalculationServiceImpl implements InsuranceCalculationService {

    private final ViaCepClient viaCepClient;
    private final RiskCalculator riskCalculator;
    private final Clock clock;

    // Métricas customizadas
    private final Counter calculationsCounter;
    private final Counter viaCepFailuresCounter;
    private final Timer calculationTimer;

    private static final BigDecimal DEFAULT_STATE_MULTIPLIER = BigDecimal.ONE;

    public InsuranceCalculationServiceImpl(
            ViaCepClient viaCepClient,
            RiskCalculator riskCalculator,
            Clock clock,
            MeterRegistry meterRegistry) {

        this.viaCepClient = viaCepClient;
        this.riskCalculator = riskCalculator;
        this.clock = clock;
        
        // Registra métricas customizadas
        this.calculationsCounter = Counter.builder("insurance.calculations.total")
            .description("Total de cálculos de seguro realizados")
            .register(meterRegistry);
        
        this.viaCepFailuresCounter = Counter.builder("insurance.viacep.failures")
            .description("Total de falhas ao consultar ViaCEP")
            .register(meterRegistry);
        
        this.calculationTimer = Timer.builder("insurance.calculation.duration")
            .description("Tempo de execução do cálculo de seguro")
            .register(meterRegistry);
    }

    @Override
    public CalculationResponseDTO calculateInsurancePremium(CalculationRequestDTO request) {
        // Inicia timer de métrica
        return calculationTimer.record(() -> {
            log.info("Iniciando cálculo de seguro para CPF: {}", maskCpf(request.getDriverCpf()));
            
            // Incrementa contador de cálculos
            calculationsCounter.increment();

            // 1. Consultar localização via CEP (com cache Redis)
            BrazilianState state = getStateFromCep(request.getDriverZipCode());
            String locationDescription = state != null 
                ? state.getFullName() 
                : "Localização não identificada";

            // 2. Calcular prêmio base (5% do valor do veículo)
            BigDecimal basePremium = riskCalculator.calculateBasePremium(request.getVehicleMarketValue());
            log.debug("Prêmio base calculado: R$ {}", basePremium);

            // 3. Calcular multiplicadores de risco
            Map<String, BigDecimal> riskFactors = calculateRiskFactors(request, state);

            // 4. Calcular multiplicador total
            BigDecimal totalMultiplier = riskCalculator.multiplyFactors(
                riskFactors.values().toArray(new BigDecimal[0])
            );

            // 5. Calcular prêmio final
            BigDecimal totalPremium = basePremium
                .multiply(totalMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

            BigDecimal monthlyPremium = totalPremium
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

            // 6. Determinar perfil de risco
            String riskProfile = riskCalculator.getRiskProfile(totalMultiplier);

            // 7. Construir resposta
            String vehicleDescription = String.format("%s %s %d/%d",
                request.getVehicleBrand(),
                request.getVehicleModel(),
                request.getVehicleManufacturingYear(),
                request.getVehicleModelYear()
            );

            LocalDate today = LocalDate.now(clock);
            int driverAge = Period.between(request.getDriverBirthDate(), today).getYears();
            int drivingExperience = Period.between(request.getDriverLicenseIssueDate(), today).getYears();

            log.info("Cálculo concluído. Prêmio total: R$ {} | Perfil: {}", totalPremium, riskProfile);

            return CalculationResponseDTO.builder()
                .quoteId(UUID.randomUUID().toString())
                .calculatedAt(LocalDateTime.now())
                .basePremium(basePremium)
                .totalPremium(totalPremium)
                .monthlyPremium(monthlyPremium)
                .driverName(request.getDriverName())
                .driverAge(driverAge)
                .drivingExperience(drivingExperience)
                .driverLocation(locationDescription)
                .vehicleDescription(vehicleDescription)
                .vehicleValue(request.getVehicleMarketValue())
                .riskFactors(riskFactors)
                .totalRiskMultiplier(totalMultiplier)
                .riskProfile(riskProfile)
                .message(generateCustomMessage(riskProfile, totalPremium))
                .build();
        });
    }

    /**
     * Calcula todos os fatores de risco usando o RiskCalculator injetável
     */
    private Map<String, BigDecimal> calculateRiskFactors(CalculationRequestDTO request, BrazilianState state) {
        Map<String, BigDecimal> factors = new LinkedHashMap<>();

        // Fatores do condutor
        factors.put("Idade do Condutor", 
            riskCalculator.getAgeRiskMultiplier(request.getDriverBirthDate()));
        
        factors.put("Experiência ao Volante", 
            riskCalculator.getExperienceRiskMultiplier(request.getDriverLicenseIssueDate()));
        
        factors.put("Histórico de Sinistros", 
            riskCalculator.getClaimsRiskMultiplier(request.getDriverClaimsHistory()));

        // Fatores do veículo
        factors.put("Categoria do Veículo", 
            request.getVehicleCategory().getRiskMultiplier());
        
        factors.put("Tipo de Combustível", 
            request.getVehicleFuelType().getRiskMultiplier());
        
        factors.put("Condição do Veículo", 
            request.getVehicleCondition().getRiskMultiplier());
        
        factors.put("Idade do Veículo", 
            riskCalculator.getVehicleAgeMultiplier(request.getVehicleManufacturingYear()));

        // Fator regional (com fallback)
        BigDecimal stateMultiplier = state != null 
            ? state.getRiskMultiplier() 
            : DEFAULT_STATE_MULTIPLIER;
        factors.put("Localização Geográfica", stateMultiplier);

        return factors;
    }

    /**
     * Consulta o estado via ViaCEP com cache Redis e tratamento resiliente
     */
    private BrazilianState getStateFromCep(String zipCode) {
        try {
            ViaCepResponse response = viaCepClient.getAddressByZipCode(zipCode);
            return BrazilianState.fromUF(response.getState());
        } catch (ViaCepException e) {
            viaCepFailuresCounter.increment(); // Métrica de falhas
            log.warn("ViaCEP indisponível para CEP {}. Usando multiplicador padrão.", zipCode);
            return null;
        } catch (Exception e) {
            viaCepFailuresCounter.increment();
            log.error("Erro inesperado ao consultar CEP {}: {}", zipCode, e.getMessage());
            return null;
        }
    }

    private String generateCustomMessage(String riskProfile, BigDecimal totalPremium) {
        return switch (riskProfile) {
            case "BAIXO" -> "Parabéns! Você possui um excelente perfil de risco. " +
                           "Aproveite condições especiais em nosso seguro.";
            case "MÉDIO" -> "Seu perfil de risco é equilibrado. " +
                           "Temos coberturas adequadas para suas necessidades.";
            case "ALTO" -> "Identificamos alguns fatores de risco elevados. " +
                          "Recomendamos coberturas abrangentes para maior proteção.";
            default -> "Cotação calculada com sucesso.";
        };
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "***";
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11)
        );
    }
}
