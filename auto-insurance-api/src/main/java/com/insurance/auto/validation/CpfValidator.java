package com.insurance.auto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<ValidCpf, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) return false;

        String cleaned = cpf.replaceAll("[^0-9]", "");
        if (cleaned.length() != 11) return false;

        // Rejeita sequências com todos os dígitos iguais (ex: 111.111.111-11)
        if (cleaned.chars().distinct().count() == 1) return false;

        // Valida primeiro dígito verificador
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cleaned.charAt(i)) * (10 - i);
        }
        int firstDigit = 11 - (sum % 11);
        if (firstDigit >= 10) firstDigit = 0;
        if (firstDigit != Character.getNumericValue(cleaned.charAt(9))) return false;

        // Valida segundo dígito verificador
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cleaned.charAt(i)) * (11 - i);
        }
        int secondDigit = 11 - (sum % 11);
        if (secondDigit >= 10) secondDigit = 0;
        return secondDigit == Character.getNumericValue(cleaned.charAt(10));
    }
}
