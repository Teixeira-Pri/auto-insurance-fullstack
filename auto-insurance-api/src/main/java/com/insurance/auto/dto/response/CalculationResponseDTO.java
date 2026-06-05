package com.insurance.auto.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class CalculationResponseDTO {

    private String quoteId;
    private LocalDateTime calculatedAt;
    private BigDecimal basePremium;
    private BigDecimal totalPremium;
    private BigDecimal monthlyPremium;
    private String driverName;
    private Integer driverAge;
    private Integer drivingExperience;
    private String driverLocation;
    private String vehicleDescription;
    private BigDecimal vehicleValue;
    private Map<String, BigDecimal> riskFactors;
    private BigDecimal totalRiskMultiplier;
    private String riskProfile;
    private String message;
}
