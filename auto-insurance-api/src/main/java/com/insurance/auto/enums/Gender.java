package com.insurance.auto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Gender {
    MALE("Masculino"),
    FEMALE("Feminino"),
    OTHER("Outro"),
    PREFER_NOT_TO_SAY("Prefiro não informar");

    private final String description;
}
