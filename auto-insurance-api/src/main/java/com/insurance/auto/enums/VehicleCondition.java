package com.insurance.auto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum VehicleCondition {
    NEW("Novo",       new BigDecimal("0.95")),
    GOOD("Bom",       new BigDecimal("1.05")),
    FAIR("Regular",   new BigDecimal("1.15")),
    POOR("Precário",  new BigDecimal("1.30"));

    private final String description;
    private final BigDecimal riskMultiplier;
}
