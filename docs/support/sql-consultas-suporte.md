# Consultas SQL de apoio ao Suporte

Este documento reúne consultas SQL (PostgreSQL — `spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect`) úteis para investigação de chamados, **baseadas exclusivamente nas tabelas reais do projeto**, geradas pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`) a partir das entidades `Driver` e `Vehicle`.

---

## ⚠️ Antes de usar: o que existe (e o que não existe) no banco

| Tabela | Origem | O que contém |
|---|---|---|
| `drivers` | `entity/Driver.java` | Dados cadastrais do condutor: `id`, `name`, `cpf`, `birth_date`, `gender`, `zip_code`, `license_issue_date`, `email`, `phone`, `claims_history`, `created_at` |
| `vehicles` | `entity/Vehicle.java` | Dados cadastrais do veículo: `id`, `brand`, `model`, `manufacturing_year`, `model_year`, `fuel_type`, `category`, `condition`, `market_value`, `license_plate`, `created_at` |

**Não existe** uma tabela de "cotações" (`quotes`/`calculations`). O endpoint `POST /api/v1/insurance/calculate` é **stateless**: recebe os dados, calcula o prêmio (incluindo `quoteId`, `riskFactors`, `riskProfile`, etc.) e retorna tudo na resposta HTTP, **sem persistir o resultado**.

Além disso, **não há relacionamento (FK)** entre `drivers` e `vehicles` — são entidades independentes no modelo atual.

Isso impacta diretamente como o suporte deve interpretar "buscar cotações recentes" e "validar status da cotação" (ver seções 2 e 4).

---

## 1. Buscar cliente (condutor) por identificador

### Por ID interno
```sql
SELECT id, name, cpf, birth_date, gender, zip_code,
       license_issue_date, email, phone, claims_history, created_at
FROM drivers
WHERE id = :id;
```

### Por CPF
```sql
SELECT id, name, cpf, birth_date, gender, zip_code,
       license_issue_date, email, phone, claims_history, created_at
FROM drivers
WHERE cpf = '12345678909';
```

> O CPF é armazenado sem formatação (apenas 11 dígitos, conforme `@Column(length = 11)` em `Driver.java`). Ao registrar o CPF em um chamado, **mascare** o valor (ex.: `123.***.***-09`).

---

## 2. Buscar "cotações" recentes (cadastros recentes)

Como **não existe** tabela de cotações persistidas, a consulta abaixo lista os **cadastros mais recentes** de condutores e veículos — útil para confirmar se um cadastro foi criado recentemente (por exemplo, se outro fluxo do sistema grava esses dados):

```sql
-- Condutores cadastrados recentemente
SELECT id, name, cpf, created_at
FROM drivers
ORDER BY created_at DESC
LIMIT 20;

-- Veículos cadastrados recentemente
SELECT id, brand, model, license_plate, created_at
FROM vehicles
ORDER BY created_at DESC
LIMIT 20;
```

> Para encontrar o **resultado de uma cotação específica** (prêmio calculado, perfil de risco, `quoteId`), a fonte correta **não é o banco de dados**, e sim os **logs estruturados da aplicação**, correlacionados pelo Trace ID (ver [base-conhecimento.md](base-conhecimento.md#3-como-identificar-uma-requisição-pelo-trace-id)).

---

## 3. Verificar dados usados no cálculo

Estas consultas recriam, a partir dos dados cadastrais, os **valores de entrada** que o `RiskCalculator` utilizaria para calcular os multiplicadores de risco — úteis para conferir se o cálculo retornado pela API é coerente com o cadastro.

```sql
-- Dados do condutor + idade e tempo de habilitação na data atual
SELECT
  id,
  name,
  cpf,
  birth_date,
  DATE_PART('year', AGE(CURRENT_DATE, birth_date))         AS idade_atual,
  license_issue_date,
  DATE_PART('year', AGE(CURRENT_DATE, license_issue_date)) AS anos_habilitado,
  claims_history
FROM drivers
WHERE cpf = '12345678909';
```

```sql
-- Dados do veículo + idade do veículo na data atual
SELECT
  id,
  brand,
  model,
  manufacturing_year,
  model_year,
  (DATE_PART('year', CURRENT_DATE) - manufacturing_year) AS idade_veiculo,
  fuel_type,
  category,
  condition,
  market_value,
  license_plate
FROM vehicles
WHERE license_plate = 'ABC1D23';
```

Com `idade_atual`, `anos_habilitado`, `claims_history`, `idade_veiculo`, `fuel_type`, `category` e `condition`, é possível recalcular manualmente cada um dos 8 multiplicadores de risco (tabela completa em [incident-003-classificacao-risco-incorreta.md](incident-003-classificacao-risco-incorreta.md#2-regra-de-negócio-esperada)) e comparar com o campo `riskFactors` retornado pela API.

---

## 4. Validar status ou resultado da cotação

Como o resultado do cálculo (`quoteId`, `totalPremium`, `riskProfile`, `riskFactors`, status HTTP) **não é persistido**, "validar o status/resultado de uma cotação" depende de uma combinação de fontes:

1. **Resposta original da API** (se o usuário/sistema cliente guardou o JSON retornado).
2. **Logs estruturados**, correlacionados por `traceId` — registram o início (`Iniciando cálculo de seguro para CPF: ...`) e o fim (`Cálculo concluído. Prêmio total: R$ ... | Perfil: ...`) do processamento.
3. **Reprodução do cálculo**: reenviar o mesmo payload para `POST /api/v1/insurance/calculate` reproduz o mesmo resultado, **desde que a data atual não tenha alterado** as faixas dependentes de tempo (idade do condutor, tempo de habilitação, idade do veículo — ver [incident-003](incident-003-classificacao-risco-incorreta.md)).

Como apoio, a consulta abaixo confirma se o **cadastro do condutor/veículo** usado na cotação existe e está consistente com o payload reportado:

```sql
SELECT d.id   AS driver_id,
       d.name,
       d.cpf,
       d.claims_history,
       v.id   AS vehicle_id,
       v.brand,
       v.model,
       v.license_plate,
       v.market_value
FROM drivers d
FULL OUTER JOIN vehicles v ON v.license_plate = 'ABC1D23'
WHERE d.cpf = '12345678909'
   OR v.license_plate = 'ABC1D23';
```

> O `FULL OUTER JOIN` aqui é apenas para trazer condutor e veículo em uma única consulta de apoio — **não representa um relacionamento real no modelo de dados** (não há FK entre `drivers` e `vehicles`).

---

## 5. Apoiar investigação de divergência de perfil de risco

Consulta combinada para reunir, de uma vez, todos os dados de entrada relevantes para o cálculo de risco de um condutor e de um veículo específicos — usada em conjunto com a tabela de regras de [incident-003](incident-003-classificacao-risco-incorreta.md):

```sql
SELECT
  -- Dados do condutor
  d.cpf,
  d.birth_date,
  DATE_PART('year', AGE(CURRENT_DATE, d.birth_date))         AS idade_atual,
  d.license_issue_date,
  DATE_PART('year', AGE(CURRENT_DATE, d.license_issue_date)) AS anos_habilitado,
  d.claims_history,
  d.zip_code,

  -- Dados do veículo
  v.license_plate,
  v.manufacturing_year,
  (DATE_PART('year', CURRENT_DATE) - v.manufacturing_year)   AS idade_veiculo,
  v.category,
  v.fuel_type,
  v.condition,
  v.market_value
FROM drivers d, vehicles v
WHERE d.cpf = '12345678909'
  AND v.license_plate = 'ABC1D23';
```

Roteiro de uso:

1. Execute a consulta acima para o condutor/veículo do chamado.
2. Para cada coluna retornada, identifique a faixa correspondente na tabela de regras (seção 2 de [incident-003](incident-003-classificacao-risco-incorreta.md#2-regra-de-negócio-esperada)) e anote o multiplicador esperado.
3. Compare cada multiplicador esperado com o valor correspondente em `riskFactors` na resposta da API.
4. Se todos os valores baterem, mas o `riskProfile` final estiver diferente do esperado, verifique o **multiplicador total** (`totalRiskMultiplier`) contra os limites de classificação (`< 1.20` BAIXO, `1.20`–`<1.80` MÉDIO, `≥ 1.80` ALTO).
5. Qualquer divergência entre o valor calculado manualmente e o valor retornado pela API é candidata a escalonamento (ver [base-conhecimento.md](base-conhecimento.md#6-quando-escalar-um-chamado-para-desenvolvimento)).

---

## Boas práticas ao executar consultas de suporte

- Utilize sempre uma conexão **somente leitura**, quando disponível, para evitar alterações acidentais.
- Nunca compartilhe CPFs, e-mails ou telefones completos em tickets ou prints — utilize máscaras (ex.: `123.***.***-09`).
- Limite os resultados (`LIMIT`) ao investigar tabelas que podem crescer (ex.: listagens de cadastros recentes).
- Caso uma consulta nova e útil seja criada durante um chamado, considere adicioná-la a este arquivo.
