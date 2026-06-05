package com.insurance.auto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum FuelType {
    ELECTRIC("Elétrico", new BigDecimal("0.80")),
    HYBRID("Híbrido",    new BigDecimal("0.90")),
    ETHANOL("Etanol",    new BigDecimal("0.95")),
    FLEX("Flex",         new BigDecimal("1.00")),
    GASOLINE("Gasolina", new BigDecimal("1.00")),
    DIESEL("Diesel",     new BigDecimal("1.10"));

    private final String description;
    private final BigDecimal riskMultiplier;
}
