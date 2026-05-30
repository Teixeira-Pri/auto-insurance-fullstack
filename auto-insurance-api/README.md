# Auto Insurance Premium Calculation API

## 📋 Descrição do Projeto

API REST profissional para cálculo de prêmio de seguro automotivo, desenvolvida com **Spring Boot 3+**, seguindo princípios de **Clean Code**, **SOLID** e boas práticas de arquitetura.

## 🚀 Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **PostgreSQL**
- **Bean Validation**
- **Swagger/OpenAPI 3**
- **Lombok**
- **JUnit 5** + **Mockito**
- **AssertJ**
- **JaCoCo** (cobertura de testes)

## 🏗️ Arquitetura do Projeto

```
com.insurance.auto
├── config/                  # Configurações (OpenAPI, Cache)
├── controller/              # REST Controllers
├── dto/                     # Request/Response DTOs
│   ├── request/
│   ├── response/
│   └── viacep/
├── entity/                  # JPA Entities
├── enums/                   # Enumerações de domínio
├── exception/               # Custom Exceptions e Handler
├── repository/              # Spring Data JPA Repositories
├── service/                 # Camada de serviço
│   └── impl/
├── util/                    # Utilitários (RiskCalculator)
├── validation/              # Validações customizadas
└── client/                  # Clientes de APIs externas
```

## 📊 Modelo de Dados

### Entidades Principais

1. **Driver** - Condutor do veículo
   - CPF, nome, data de nascimento, gênero
   - CEP (integração com ViaCEP)
   - Histórico de sinistros
   - Data da primeira habilitação

2. **Vehicle** - Veículo a ser segurado
   - Marca, modelo, ano
   - Categoria (sedan, SUV, esportivo, etc.)
   - Tipo de combustível
   - Valor de mercado
   - Condição (novo, bom, regular, etc.)

### Enums

- `Gender` - Gênero do condutor
- `BrazilianState` - Estados com multiplicadores regionais
- `FuelType` - Tipos de combustível
- `VehicleCategory` - Categorias de veículo
- `VehicleCondition` - Estado de conservação

## 🧮 Lógica de Cálculo

### Prêmio Base
```
Prêmio Base = 5% do valor do veículo
```

### Multiplicadores Aplicados

| Fator | Variação | Impacto |
|-------|----------|---------|
| **Idade do Condutor** | < 25 anos: +30%<br>25-60 anos: padrão<br>> 60 anos: +15% | 0.85 - 1.30 |
| **Experiência** | < 2 anos: +25%<br>2-5 anos: +10%<br>10+ anos: -5% | 0.95 - 1.25 |
| **Histórico Sinistros** | 0 sinistros: -10%<br>1 sinistro: +10%<br>4+ sinistros: +60% | 0.90 - 1.60 |
| **Categoria Veículo** | Compacto: padrão<br>Esportivo: +60%<br>Moto: +100% | 1.00 - 2.00 |
| **Combustível** | Elétrico: -20%<br>Gasolina: padrão<br>Diesel: +10% | 0.80 - 1.10 |
| **Condição Veículo** | Novo: -5%<br>Bom: +5%<br>Precário: +30% | 0.95 - 1.30 |
| **Idade Veículo** | Novo: -5%<br>5-10 anos: +10%<br>15+ anos: +35% | 0.95 - 1.35 |
| **Localização** | RJ: +30%<br>SP: +25%<br>Estados Norte: -10% | 0.90 - 1.30 |

### Fórmula Final
```
Prêmio Total = Prêmio Base × Π(Multiplicadores)
Prêmio Mensal = Prêmio Total ÷ 12
```

## 🔌 Integração Externa

### ViaCEP API
- **Endpoint:** `https://viacep.com.br/ws/{cep}/json/`
- **Propósito:** Obter estado do condutor para aplicar multiplicador regional
- **Resiliência:** Fallback para multiplicador padrão (1.0) em caso de falha
- **Cache:** Respostas são cacheadas para reduzir requisições

## 🛡️ Validações Implementadas

### Validações Customizadas
- **@ValidCpf** - Valida CPF com dígitos verificadores
- Idade mínima de 18 anos
- Data de CNH posterior aos 18 anos do condutor
- CEP com 8 dígitos
- Valores monetários em BigDecimal

### Validações Bean Validation
- `@NotNull`, `@NotBlank`
- `@Email`, `@Pattern`
- `@Min`, `@Max`
- `@Past`, `@PastOrPresent`
- `@DecimalMin`, `@DecimalMax`

## 📡 Endpoints da API

### POST /api/v1/insurance/calculate
Calcula o prêmio de seguro

**Request Body:**
```json
{
  "driverName": "João Silva",
  "driverCpf": "12345678909",
  "driverBirthDate": "1985-03-15",
  "driverGender": "MALE",
  "driverZipCode": "01310100",
  "driverLicenseIssueDate": "2005-06-20",
  "driverEmail": "joao.silva@email.com",
  "driverPhone": "11987654321",
  "driverClaimsHistory": 0,
  "vehicleBrand": "Toyota",
  "vehicleModel": "Corolla",
  "vehicleManufacturingYear": 2020,
  "vehicleModelYear": 2021,
  "vehicleFuelType": "FLEX",
  "vehicleCategory": "SEDAN",
  "vehicleCondition": "GOOD",
  "vehicleMarketValue": 85000.00,
  "vehicleLicensePlate": "ABC1D23"
}
```

**Response (200 OK):**
```json
{
  "quoteId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "calculatedAt": "2026-05-22T14:30:00",
  "basePremium": 4250.00,
  "totalPremium": 5865.75,
  "monthlyPremium": 488.81,
  "driverName": "João Silva",
  "driverAge": 41,
  "drivingExperience": 21,
  "driverLocation": "São Paulo",
  "vehicleDescription": "Toyota Corolla 2020/2021",
  "vehicleValue": 85000.00,
  "riskFactors": {
    "Idade do Condutor": 1.00,
    "Experiência ao Volante": 0.95,
    "Histórico de Sinistros": 0.90,
    "Categoria do Veículo": 1.05,
    "Tipo de Combustível": 1.00,
    "Condição do Veículo": 1.05,
    "Idade do Veículo": 1.10,
    "Localização Geográfica": 1.25
  },
  "totalRiskMultiplier": 1.3802,
  "riskProfile": "MÉDIO",
  "message": "Seu perfil de risco é equilibrado..."
}
```

### GET /api/v1/insurance/health
Health check do serviço

## 🧪 Testes

### Cobertura de Testes
- **InsuranceCalculationServiceImplTest**
  - Cálculo com sucesso (ViaCEP funcionando)
  - Fallback quando ViaCEP falha
  - Condutor de alto risco
  - Condutor de baixo risco
  - Veículo elétrico
  - Motocicleta

- **RiskCalculatorTest**
  - Todos os multiplicadores individuais
  - Multiplicação de fatores
  - Classificação de perfil de risco

### Executar Testes
```bash
# Todos os testes
mvn test

# Com relatório de cobertura
mvn clean test jacoco:report

# Ver relatório
open target/site/jacoco/index.html
```

## 🚀 Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Configurar Banco de Dados
```sql
CREATE DATABASE insurance_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE insurance_db TO postgres;
```

### 2. Configurar Application Properties
Edite `src/main/resources/application.properties` com suas credenciais do PostgreSQL.

### 3. Compilar e Executar
```bash
# Compilar
mvn clean install

# Executar
mvn spring-boot:run

# Ou via JAR
java -jar target/auto-insurance-api-1.0.0.jar
```

### 4. Acessar Documentação Swagger
```
http://localhost:8080/swagger-ui.html
```

## 📚 Documentação OpenAPI

A API está totalmente documentada com Swagger/OpenAPI 3. Acesse:
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs JSON:** http://localhost:8080/api-docs

## 🔒 Tratamento de Erros

### Códigos de Resposta
- **200** - Sucesso
- **400** - Erro de validação
- **422** - Erro de regra de negócio
- **500** - Erro interno do servidor
- **502** - Erro ao consultar serviço externo (ViaCEP)

### Exemplo de Erro de Validação (400)
```json
{
  "timestamp": "2026-05-22T14:30:00",
  "status": 400,
  "error": "Validation Error",
  "message": "Erro de validação nos dados enviados",
  "path": "/api/v1/insurance/calculate",
  "fieldErrors": [
    {
      "field": "driverCpf",
      "message": "CPF inválido. Verifique os dígitos verificadores",
      "rejectedValue": "12345678900"
    }
  ]
}
```

## 📁 Estrutura de Arquivos do Projeto

```
auto-insurance-api/
├── src/
│   ├── main/
│   │   ├── java/com/insurance/auto/
│   │   │   ├── AutoInsuranceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── OpenApiConfig.java
│   │   │   │   └── CacheConfig.java
│   │   │   ├── controller/
│   │   │   │   └── InsuranceCalculationController.java
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   │   └── CalculationRequestDTO.java
│   │   │   │   ├── response/
│   │   │   │   │   └── CalculationResponseDTO.java
│   │   │   │   └── viacep/
│   │   │   │       └── ViaCepResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── Driver.java
│   │   │   │   └── Vehicle.java
│   │   │   ├── enums/
│   │   │   │   ├── Gender.java
│   │   │   │   ├── BrazilianState.java
│   │   │   │   ├── FuelType.java
│   │   │   │   ├── VehicleCategory.java
│   │   │   │   └── VehicleCondition.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── ValidationErrorResponse.java
│   │   │   │   ├── BusinessException.java
│   │   │   │   └── ViaCepException.java
│   │   │   ├── repository/
│   │   │   │   ├── DriverRepository.java
│   │   │   │   └── VehicleRepository.java
│   │   │   ├── service/
│   │   │   │   ├── InsuranceCalculationService.java
│   │   │   │   └── impl/
│   │   │   │       └── InsuranceCalculationServiceImpl.java
│   │   │   ├── util/
│   │   │   │   └── RiskCalculator.java
│   │   │   ├── validation/
│   │   │   │   ├── ValidCpf.java
│   │   │   │   └── CpfValidator.java
│   │   │   └── client/
│   │   │       └── ViaCepClient.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/insurance/auto/
│           ├── service/impl/
│           │   └── InsuranceCalculationServiceImplTest.java
│           └── util/
│               └── RiskCalculatorTest.java
├── pom.xml
└── README.md
```

## 🎯 Principais Características

✅ **Arquitetura Limpa** - Separação clara de responsabilidades  
✅ **SOLID** - Princípios aplicados em todo o código  
✅ **Clean Code** - Código legível e manutenível  
✅ **Validações Robustas** - Bean Validation + customizadas  
✅ **Tratamento de Exceções** - Global Exception Handler  
✅ **Resiliência** - Fallback para serviços externos  
✅ **Testes Abrangentes** - JUnit 5 + Mockito  
✅ **Documentação Completa** - Swagger/OpenAPI  
✅ **BigDecimal** - Precisão em valores monetários  
✅ **Logs Estruturados** - SLF4J  

## 📝 Licença

Este projeto está sob a licença MIT.

## 👥 Autores

Desenvolvido como projeto educacional de API REST profissional com Spring Boot.

---

**Versão:** 1.0.0  
**Data:** Maio 2026
