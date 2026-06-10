# Incidente 003 — Classificação de risco incorreta

| Campo | Valor |
|---|---|
| **ID do incidente** | INC-003 |
| **Categoria** | Divergência de regra de negócio / Classificação de perfil de risco |
| **Componente afetado** | `auto-insurance-api` — `RiskCalculator`, `InsuranceCalculationServiceImpl` |
| **Severidade sugerida** | Média (requer análise antes de definir se é bug ou comportamento esperado) |

> Caso simulado, baseado nas regras reais implementadas em `RiskCalculator.java` (multiplicadores de idade, experiência, sinistros, veículo e localização) e na classificação final em `getRiskProfile()`.

---

## 1. Cenário do usuário

Um usuário (ou um corretor interno) relata que, ao simular novamente uma cotação para o mesmo perfil de cliente, o **perfil de risco** retornado mudou de `"BAIXO"` para `"MÉDIO"`, sem que o usuário tenha alterado os dados informados — ou pelo menos é o que ele acredita.

> "Semana passada o sistema classificou meu cliente como risco BAIXO. Hoje refiz a mesma simulação e veio como MÉDIO. Os dados são os mesmos, isso parece um erro de classificação."

---

## 2. Regra de negócio esperada

O perfil de risco é determinado pelo **multiplicador total de risco** (`totalRiskMultiplier`), que é o produto de **8 multiplicadores individuais** (`RiskCalculator.multiplyFactors`):

| Fator | Regra | Faixas de multiplicador |
|---|---|---|
| Idade do condutor | `< 25`: 1.30 · `25–60`: 1.00 · `> 60`: 1.15 | 0.85 – 1.30 (variação) |
| Experiência (CNH) | `< 2 anos`: 1.25 · `2–5 anos`: 1.10 · `5–10 anos`: 1.00 · `≥ 10 anos`: 0.95 | 0.95 – 1.25 |
| Histórico de sinistros | `0`: 0.90 · `1`: 1.10 · `2`: 1.25 · `3`: 1.40 · `≥ 4`: 1.60 | 0.90 – 1.60 |
| Categoria do veículo | Compacto 1.00 · Sedan 1.05 · SUV 1.10 · Picape 1.15 · Minivan 1.10 · Caminhão 1.20 · Esportivo 1.60 · Moto 2.00 | 1.00 – 2.00 |
| Combustível | Elétrico 0.80 · Híbrido 0.90 · Etanol 0.95 · Flex/Gasolina 1.00 · Diesel 1.10 | 0.80 – 1.10 |
| Condição do veículo | Novo 0.95 · Bom 1.05 · Regular 1.15 · Precário 1.30 | 0.95 – 1.30 |
| Idade do veículo | `≤ 1`: 0.95 · `≤ 5`: 1.00 · `≤ 10`: 1.10 · `≤ 15`: 1.20 · `> 15`: 1.35 | 0.95 – 1.35 |
| Localização (UF do CEP) | varia por estado (`BrazilianState`), de 0.90 (ex.: AC, AM, AP) a 1.30 (RJ) | 0.90 – 1.30 |

**Classificação final** (`getRiskProfile`):

| Multiplicador total | Perfil |
|---|---|
| `< 1.20` | **BAIXO** |
| `1.20` a `< 1.80` | **MÉDIO** |
| `≥ 1.80` | **ALTO** |

Como **idade do condutor**, **experiência de CNH** e **idade do veículo** dependem da **data atual** (`LocalDate.now(clock)`), o resultado de uma simulação **pode mudar com o tempo**, mesmo que o restante dos dados permaneça idêntico — isso é **comportamento esperado**, não um bug.

---

## 3. Dados necessários para análise

Para investigar uma divergência de classificação, o suporte deve coletar:

- [ ] Payload completo enviado em **ambas** as simulações (a anterior, classificada como `BAIXO`, e a atual, classificada como `MÉDIO`)
- [ ] `riskFactors` retornados em **ambas** as respostas (o campo `riskFactors` do `CalculationResponseDTO` traz o valor de cada um dos 8 multiplicadores individualmente)
- [ ] `totalRiskMultiplier` de cada simulação
- [ ] Data/hora de cada simulação (para avaliar se houve mudança de faixa etária, de experiência ou de idade do veículo entre as duas datas)
- [ ] CEP usado em cada simulação (para verificar se o estado retornado pelo ViaCEP foi o mesmo)

> O campo `riskFactors` é a evidência mais importante: ele permite comparar **fator a fator** qual multiplicador mudou entre as duas execuções.

---

## 4. Consulta SQL para validar informações

O resultado da cotação (incluindo `riskFactors` e `totalRiskMultiplier`) **não é persistido no banco** — é calculado e retornado na resposta HTTP (ver [sql-consultas-suporte.md](sql-consultas-suporte.md) para detalhes sobre o que é e o que não é persistido).

O suporte pode, no entanto, consultar os **dados cadastrais do condutor e do veículo** (caso existam registros em `drivers`/`vehicles`) para conferir se os dados de entrada usados na simulação batem com o cadastro, e recalcular manualmente os fatores dependentes de data:

```sql
-- Dados do condutor e cálculo de idade/experiência na data atual
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

-- Dados do veículo e cálculo da idade do veículo na data atual
SELECT
  id,
  brand,
  model,
  manufacturing_year,
  (DATE_PART('year', CURRENT_DATE) - manufacturing_year) AS idade_veiculo,
  fuel_type,
  category,
  condition,
  market_value,
  license_plate
FROM vehicles
WHERE license_plate = 'ABC1D23';
```

Com `idade_atual`, `anos_habilitado` e `idade_veiculo`, o suporte consegue recalcular manualmente os multiplicadores correspondentes (tabela da seção 2) e comparar com o `riskFactors` retornado pela API.

---

## 5. Possível causa raiz

| Hipótese | Como confirmar | É bug? |
|---|---|---|
| **Mudança de faixa por data** — o condutor fez aniversário entre as duas simulações (ex.: completou 61 anos, saindo da faixa `25–60` para `> 60`), ou o veículo "completou" mais um ano na contagem `idade do veículo` | Comparar `birth_date`/`manufacturing_year` com a data de cada simulação; recalcular pela tabela da seção 2 | **Não** — comportamento esperado e documentado |
| **Divergência no histórico de sinistros informado** — `driverClaimsHistory` foi preenchido com valor diferente entre as duas simulações (ex.: `0` vs `1`), alterando o multiplicador de `0.90` para `1.10` (variação relevante: ~22%) | Comparar o campo `driverClaimsHistory` enviado em cada payload | **Não** — erro de preenchimento, não do sistema |
| **CEP retornando UF diferente** — CEP digitado incorretamente faz o ViaCEP retornar um estado com multiplicador maior (ex.: `1.25` em SP vs `1.30` em RJ) | Comparar `driverLocation` e o multiplicador "Localização Geográfica" em `riskFactors` de cada resposta | **Não** — dado de entrada diferente |
| **Cálculo do `riskFactors` não corresponde à tabela documentada** — por exemplo, `totalRiskMultiplier` não é o produto exato dos 8 fatores individuais retornados | Multiplicar manualmente os 8 valores de `riskFactors` e comparar com `totalRiskMultiplier` | **Possível bug** — escalar para Desenvolvimento |
| **Classificação (`riskProfile`) não corresponde aos limites documentados** — ex.: `totalRiskMultiplier = 1.15` retornando `"MÉDIO"` (deveria ser `"BAIXO"`, pois `< 1.20`) | Comparar `totalRiskMultiplier` com a tabela de limites da seção 2 | **Bug confirmado** — escalar para Desenvolvimento |

---

## 6. Como documentar o incidente

Ao registrar o chamado/ticket, incluir:

1. **Os dois payloads completos** (anterior e atual), em JSON.
2. **As duas respostas completas** (`riskFactors`, `totalRiskMultiplier`, `riskProfile`).
3. **Tabela comparativa fator a fator** (uma linha por fator de risco, com o valor em cada simulação e se houve mudança).
4. **Data/hora de cada simulação** e o resultado do recálculo manual (idade, experiência, idade do veículo na data de cada simulação).
5. **Conclusão da análise**: comportamento esperado (com explicação da regra) ou divergência a confirmar com Desenvolvimento.
6. **Trace IDs** de ambas as requisições, se disponíveis nos logs (ver [incident-001](incident-001-error-calculo-cotacao.md), seção "Como usar o Trace ID").

---

## 7. Como encaminhar para Produto/Desenvolvimento

### Encaminhar para **Produto** quando:
- O comportamento está **tecnicamente correto** (cálculo bate com a regra documentada), mas **gera confusão para o usuário** — por exemplo, o usuário não entende por que o perfil mudou de uma simulação para outra.
- Sugestão: exibir, na resposta/tela de resultado, um aviso indicando que o cálculo é sensível à data atual (idade do condutor, tempo de habilitação e idade do veículo são recalculados a cada simulação).

### Encaminhar para **Desenvolvimento** quando:
- O `totalRiskMultiplier` não corresponde ao produto dos valores em `riskFactors` (erro de cálculo).
- O `riskProfile` não corresponde aos limites documentados (`< 1.20` → BAIXO, `1.20`–`<1.80` → MÉDIO, `≥ 1.80` → ALTO).
- Algum dos 8 multiplicadores individuais não corresponde à tabela de regras (seção 2) para os dados de entrada informados.

Ao escalar, anexar: payloads, respostas completas, tabela comparativa fator a fator, recálculo manual e `traceId` (quando disponível). Isso permite ao time de Desenvolvimento reproduzir o cenário rapidamente, sem precisar solicitar novas evidências ao usuário.
