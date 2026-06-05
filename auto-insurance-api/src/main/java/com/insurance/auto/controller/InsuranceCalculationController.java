package com.insurance.auto.controller;

import com.insurance.auto.dto.request.CalculationRequestDTO;
import com.insurance.auto.dto.response.CalculationResponseDTO;
import com.insurance.auto.service.InsuranceCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/insurance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Insurance Calculation", description = "Endpoints para cálculo de prêmio de seguro automotivo")
public class InsuranceCalculationController {

    private final InsuranceCalculationService insuranceCalculationService;

    @PostMapping("/calculate")
    @Operation(
        summary = "Calcular prêmio de seguro",
        description = "Calcula o prêmio anual e mensal de seguro automotivo com base no perfil do condutor e do veículo. " +
                      "Aplica múltiplos fatores de risco e integra com ViaCEP para multiplicador regional."
    )
    public ResponseEntity<CalculationResponseDTO> calculatePremium(
            @Valid @RequestBody CalculationRequestDTO request) {

        log.info("Solicitação de cálculo recebida para CPF: {}", maskCpf(request.getDriverCpf()));
        CalculationResponseDTO response = insuranceCalculationService.calculateInsurancePremium(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está ativo")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auto-insurance-api",
                "version", "1.0.0"
        ));
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 11) return "***";
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9, 11);
    }
}
