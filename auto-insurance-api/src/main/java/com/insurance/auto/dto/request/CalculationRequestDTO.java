package com.insurance.auto.dto.request;

import com.insurance.auto.enums.FuelType;
import com.insurance.auto.enums.Gender;
import com.insurance.auto.enums.VehicleCategory;
import com.insurance.auto.enums.VehicleCondition;
import com.insurance.auto.validation.ValidCpf;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CalculationRequestDTO {

    @NotBlank(message = "Nome do condutor é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String driverName;

    @NotBlank(message = "CPF é obrigatório")
    @ValidCpf
    private String driverCpf;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate driverBirthDate;

    @NotNull(message = "Gênero é obrigatório")
    private Gender driverGender;

    @NotBlank(message = "CEP é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "CEP deve conter 8 dígitos numéricos")
    private String driverZipCode;

    @NotNull(message = "Data de emissão da CNH é obrigatória")
    @PastOrPresent(message = "Data da CNH não pode ser no futuro")
    private LocalDate driverLicenseIssueDate;

    @Email(message = "E-mail inválido")
    private String driverEmail;

    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter 10 ou 11 dígitos")
    private String driverPhone;

    @NotNull(message = "Histórico de sinistros é obrigatório")
    @Min(value = 0, message = "Número de sinistros não pode ser negativo")
    @Max(value = 20, message = "Número de sinistros não pode ser superior a 20")
    private Integer driverClaimsHistory;

    @NotBlank(message = "Marca do veículo é obrigatória")
    private String vehicleBrand;

    @NotBlank(message = "Modelo do veículo é obrigatório")
    private String vehicleModel;

    @NotNull(message = "Ano de fabricação é obrigatório")
    @Min(value = 1980, message = "Ano de fabricação deve ser a partir de 1980")
    private Integer vehicleManufacturingYear;

    @NotNull(message = "Ano do modelo é obrigatório")
    private Integer vehicleModelYear;

    @NotNull(message = "Tipo de combustível é obrigatório")
    private FuelType vehicleFuelType;

    @NotNull(message = "Categoria do veículo é obrigatória")
    private VehicleCategory vehicleCategory;

    @NotNull(message = "Condição do veículo é obrigatória")
    private VehicleCondition vehicleCondition;

    @NotNull(message = "Valor de mercado é obrigatório")
    @DecimalMin(value = "1000.00", message = "Valor de mercado deve ser maior que R$ 1.000,00")
    @DecimalMax(value = "10000000.00", message = "Valor de mercado deve ser menor que R$ 10.000.000,00")
    private BigDecimal vehicleMarketValue;

    @Pattern(regexp = "[A-Z]{3}\\d[A-Z0-9]\\d{2}|[A-Z]{3}\\d{4}",
             message = "Placa inválida. Use o formato ABC1234 ou ABC1D23")
    private String vehicleLicensePlate;
}
