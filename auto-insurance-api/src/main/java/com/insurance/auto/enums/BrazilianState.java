package com.insurance.auto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum BrazilianState {
    AC("Acre",               new BigDecimal("0.90")),
    AL("Alagoas",            new BigDecimal("1.00")),
    AM("Amazonas",           new BigDecimal("0.90")),
    AP("Amapá",              new BigDecimal("0.90")),
    BA("Bahia",              new BigDecimal("1.05")),
    CE("Ceará",              new BigDecimal("1.00")),
    DF("Distrito Federal",   new BigDecimal("1.20")),
    ES("Espírito Santo",     new BigDecimal("1.10")),
    GO("Goiás",              new BigDecimal("1.05")),
    MA("Maranhão",           new BigDecimal("0.95")),
    MG("Minas Gerais",       new BigDecimal("1.10")),
    MS("Mato Grosso do Sul", new BigDecimal("1.00")),
    MT("Mato Grosso",        new BigDecimal("1.00")),
    PA("Pará",               new BigDecimal("0.90")),
    PB("Paraíba",            new BigDecimal("0.95")),
    PE("Pernambuco",         new BigDecimal("1.05")),
    PI("Piauí",              new BigDecimal("0.95")),
    PR("Paraná",             new BigDecimal("1.10")),
    RJ("Rio de Janeiro",     new BigDecimal("1.30")),
    RN("Rio Grande do Norte",new BigDecimal("0.95")),
    RO("Rondônia",           new BigDecimal("0.90")),
    RR("Roraima",            new BigDecimal("0.90")),
    RS("Rio Grande do Sul",  new BigDecimal("1.10")),
    SC("Santa Catarina",     new BigDecimal("1.05")),
    SE("Sergipe",            new BigDecimal("1.00")),
    SP("São Paulo",          new BigDecimal("1.25")),
    TO("Tocantins",          new BigDecimal("0.90"));

    private final String fullName;
    private final BigDecimal riskMultiplier;

    public static BrazilianState fromUF(String uf) {
        if (uf == null || uf.isBlank()) return null;
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(uf.trim()))
                .findFirst()
                .orElse(null);
    }
}
