package com.insurance.auto.service;

import com.insurance.auto.dto.request.CalculationRequestDTO;
import com.insurance.auto.dto.response.CalculationResponseDTO;

public interface InsuranceCalculationService {
    CalculationResponseDTO calculateInsurancePremium(CalculationRequestDTO request);
}
