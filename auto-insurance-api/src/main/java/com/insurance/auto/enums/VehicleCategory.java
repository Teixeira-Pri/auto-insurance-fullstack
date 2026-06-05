package com.insurance.auto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public enum VehicleCategory {
    COMPACT("Compacto",    new BigDecimal("1.00")),
    SEDAN("Sedan",         new BigDecimal("1.05")),
    SUV("SUV",             new BigDecimal("1.10")),
    PICKUP("Picape",       new BigDecimal("1.15")),
    MINIVAN("Minivan",     new BigDecimal("1.10")),
    TRUCK("Caminhão",      new BigDecimal("1.20")),
    SPORT("Esportivo",     new BigDecimal("1.60")),
    MOTORCYCLE("Motocicleta", new BigDecimal("2.00"));

    private final String description;
    private final BigDecimal riskMultiplier;
}
