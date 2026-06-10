# Incidente 001 — Erro ao calcular cotação

| Campo | Valor |
|---|---|
| **ID do incidente** | INC-001 |
| **Categoria** | Erro funcional / Validação de dados |
| **Componente afetado** | `auto-insurance-api` — `POST /api/v1/insurance/calculate` |
| **Severidade sugerida** | Média (caso isolado) / Alta (se recorrente em vários usuários) |

> Este documento é um **caso simulado**, construído a partir do comportamento real do projeto (validações do `CalculationRequestDTO`, `GlobalExceptionHandler` e `CpfValidator`). O objetivo é demonstrar o fluxo de análise que um analista de Suporte/Sustentação executaria diante de um chamado real.

---

## 1. Cenário do usuário

Um usuário acessa o frontend (`http://localhost:3000`), preenche o formulário de cotação de seguro automotivo e clica em **"Calcular Prêmio"**. A tela exibe uma mensagem de erro e nenhuma cotação é apresentada.

O usuário abre um chamado relatando:

> "Preenchi todos os campos do formulário, cliquei em calcular e apareceu uma mensagem de erro. Não consegui ver o valor do meu seguro."

---

## 2. Impacto

- O usuário não consegue concluir a simulação de cotação (funcionalidade principal do produto).
- Se o erro ocorrer apenas para este usuário, o impacto é **individual** (provável problema no dado informado).
- Se o mesmo erro ocorrer para múltiplos usuários ao mesmo tempo, o impacto é **sistêmico** (possível indisponibilidade da API, do banco de dados ou do Redis) e deve ser tratado com prioridade mais alta.

---

## 3. Prioridade

| Critério | Classificação |
|---|---|
| Usuários afetados | 1 usuário → impacto pontual |
| Funcionalidade | Núcleo do produto (cálculo de cotação) |
| Workaround disponível | Sim, na maioria dos casos (corrigir o dado e reenviar) |
| **Prioridade sugerida** | **P3 (Média)** para caso isolado de validação. Reclassificar para **P2 (Alta)** se houver volume de chamados semelhantes em curto intervalo de tempo (indício de problema sistêmico). |

---

## 4. Evidências coletadas

Antes de investigar, o suporte deve coletar do usuário (ou via DevTools do navegador, aba **Network**):

- [ ] Print da tela com a mensagem de erro
- [ ] Horário aproximado em que o erro ocorreu
- [ ] Payload enviado (corpo da requisição `POST /api/v1/insurance/calculate`)
- [ ] Status HTTP retornado e corpo da resposta de erro
- [ ] CPF utilizado no teste (mascarado ao registrar no chamado, ex.: `123.***.***-09`)

---

## 5. Endpoint envolvido

```
POST /api/v1/insurance/calculate
Content-Type: application/json
```

Documentado no Swagger/OpenAPI em `http://localhost:8080/swagger-ui.html` (tag **Insurance Calculation**).

---

## 6. Payload de exemplo (reproduzindo o erro)

Exemplo de payload com **CPF inválido** (dígitos verificadores incorretos) — uma das causas mais comuns de erro reportado pelo usuário como "deu erro ao calcular":

```json
{
  "driverName": "Maria Souza",
  "driverCpf": "11111111111",
  "driverBirthDate": "1990-04-10",
  "driverGender": "FEMALE",
  "driverZipCode": "20040020",
  "driverLicenseIssueDate": "2010-02-01",
  "driverEmail": "maria.souza@email.com",
  "driverPhone": "21988887777",
  "driverClaimsHistory": 0,
  "vehicleBrand": "Honda",
  "vehicleModel": "Civic",
  "vehicleManufacturingYear": 2019,
  "vehicleModelYear": 2020,
  "vehicleFuelType": "FLEX",
  "vehicleCategory": "SEDAN",
  "vehicleCondition": "GOOD",
  "vehicleMarketValue": 95000.00,
  "vehicleLicensePlate": "ABC1D23"
}
```

---

## 7. Status HTTP esperado vs. recebido

| | Status | Observação |
|---|---|---|
| **Esperado** | `200 OK` | Cotação calculada com sucesso, retornando `CalculationResponseDTO` |
| **Recebido** | `400 Bad Request` | Erro de validação (`ValidationErrorResponse`) |

Resposta recebida (exemplo real, conforme `GlobalExceptionHandler.handleValidationException`):

```json
{
  "timestamp": "2026-06-10T14:32:10",
  "status": 400,
  "error": "Validation Error",
  "message": "Erro de validação nos dados enviados",
  "path": "/api/v1/insurance/calculate",
  "fieldErrors": [
    {
      "field": "driverCpf",
      "message": "CPF inválido. Verifique os dígitos verificadores",
      "rejectedValue": "11111111111"
    }
  ]
}
```

> Outros campos que costumam gerar `400 Bad Request` semelhante: `driverZipCode` (precisa ter exatamente 8 dígitos numéricos), `vehicleMarketValue` (deve estar entre `1000.00` e `10000000.00`), `driverBirthDate` (deve ser uma data no passado) e `vehicleLicensePlate` (formato `ABC1234` ou `ABC1D23`).

---

## 8. Como consultar os logs

A API está configurada (`application.properties`) com o padrão de log:

```properties
logging.level.com.insurance.auto=DEBUG
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

Isso significa que **toda** linha de log da aplicação inclui o nome da aplicação, o `traceId` e o `spanId` da requisição. Exemplo de log gerado para esta requisição:

```
2026-06-10T14:32:09.987-03:00  INFO [auto-insurance-api,6f8a1b2c3d4e5f6071829304a1b2c3d,a1b2c3d4e5f60718] 12345 --- [nio-8080-exec-3] c.i.a.c.InsuranceCalculationController : Solicitação de cálculo recebida para CPF: 111.***.***-11
2026-06-10T14:32:10.012-03:00  WARN [auto-insurance-api,6f8a1b2c3d4e5f6071829304a1b2c3d,a1b2c3d4e5f60718] 12345 --- [nio-8080-exec-3] o.s.w.b.s.s.DefaultHandlerExceptionResolver : Resolved [MethodArgumentNotValidException: ...]
```

**Como localizar o log do chamado:**

1. Pegue o horário aproximado informado pelo usuário (ex.: "14:32 de 10/06/2026").
2. Filtre o arquivo/stream de log da aplicação por esse intervalo de tempo.
3. Procure pela linha `Solicitação de cálculo recebida para CPF: ...` — o CPF é mascarado (`123.***.***-09`), o que já ajuda a confirmar se é o mesmo usuário.
4. Quando a validação falha (como neste caso), o Spring registra um `WARN`/`DEBUG` com `MethodArgumentNotValidException`, indicando o campo rejeitado.

---

## 9. Como usar o Trace ID

Cada requisição HTTP recebida pela API gera automaticamente um **Trace ID** e um **Span ID** (via Micrometer Tracing + Brave, configurados em `pom.xml` e `application.properties`, com `management.tracing.sampling.probability=1.0` — ou seja, 100% das requisições são rastreadas).

Passo a passo para o suporte:

1. Localize **uma** linha de log relacionada ao chamado (pelo horário e CPF mascarado), conforme o passo anterior.
2. Copie o valor do `traceId` entre colchetes — ex.: `6f8a1b2c3d4e5f6071829304a1b2c3d`.
3. Filtre **todas** as linhas de log que contenham esse mesmo `traceId`. Isso reconstrói o ciclo de vida completo daquela requisição específica (recebimento, consulta ao ViaCEP, cálculo, resposta).
4. Se o Zipkin estiver disponível (`http://localhost:9411`, conforme `management.zipkin.tracing.endpoint`), é possível buscar o mesmo `traceId` na interface do Zipkin para visualizar a linha do tempo (spans) da requisição.

> **Observação importante:** atualmente a API não retorna o `traceId` no corpo da resposta nem em um header HTTP para o cliente. Ou seja, o usuário final não tem esse identificador — o suporte precisa correlacionar pelo **horário + CPF mascarado** para encontrar o `traceId` correspondente nos logs. Uma melhoria futura seria expor um header `X-Trace-Id` na resposta (ver sugestão na [base de conhecimento](base-conhecimento.md)).

---

## 10. Consulta SQL de apoio

O cálculo em si **não é persistido** no banco (ver detalhes em [sql-consultas-suporte.md](sql-consultas-suporte.md)). Porém, é possível verificar se o **CPF informado já está cadastrado** na tabela `drivers` — o que ajuda a confirmar se o usuário é um cliente conhecido ou se é um teste com dado fictício:

```sql
SELECT id, name, cpf, birth_date, claims_history, created_at
FROM drivers
WHERE cpf = '11111111111';
```

Se a consulta não retornar linhas, é um indício de que o CPF informado é fictício/teste — reforçando a hipótese de erro de validação de entrada (e não um defeito no sistema).

---

## 11. Causa provável

Para o payload de exemplo, a causa é **erro de validação de entrada (HTTP 400)**:

- O campo `driverCpf` não passou na validação `@ValidCpf` (`CpfValidator`), pois `11111111111` possui todos os dígitos iguais — regra explícita de rejeição no validador (`cleaned.chars().distinct().count() == 1`).
- Esse é um **erro de regra de negócio/validação de entrada**, não um defeito técnico da API.

Outras causas prováveis para "erro ao calcular cotação" reportado pelo usuário:

| Causa provável | Como confirmar |
|---|---|
| CPF com dígito verificador inválido | `fieldErrors[].field = "driverCpf"`, status `400` |
| CEP fora do padrão (≠ 8 dígitos) | `fieldErrors[].field = "driverZipCode"`, status `400` |
| Valor do veículo fora da faixa (R$ 1.000 a R$ 10.000.000) | `fieldErrors[].field = "vehicleMarketValue"`, status `400` |
| Data de nascimento no futuro | `fieldErrors[].field = "driverBirthDate"`, status `400` |
| Falha inesperada (NullPointerException, etc.) | status `500`, log com `ERROR` e stack trace, mensagem genérica "Ocorreu um erro interno" |

---

## 12. Workaround

1. Identificar, no `fieldErrors` da resposta `400`, qual campo foi rejeitado e por quê.
2. Orientar o usuário a corrigir o campo indicado (ex.: reinformar o CPF correto, ajustar o CEP para 8 dígitos, revisar o valor do veículo).
3. Solicitar que o usuário refaça a simulação após a correção.
4. Caso o erro seja `500 Internal Server Error` (não relacionado à validação), **não há workaround para o usuário** — o caso deve ser escalado (ver seção 13).

---

## 13. Solução definitiva sugerida

- **Se for erro de validação (400):** não há "correção" no backend — o comportamento está correto e documentado (ver `CalculationRequestDTO` e Swagger). A solução é orientar o usuário/UX do frontend a exibir mensagens de validação mais claras (`fieldErrors[].message` já traz o texto em português, pronto para exibição).
- **Se for erro técnico (500):** a solução definitiva depende da causa raiz identificada nos logs (ex.: corrigir tratamento de um caso de borda no `RiskCalculator` ou no `InsuranceCalculationServiceImpl`). Deve ser registrada como tarefa para o time de Desenvolvimento, anexando: payload, `traceId`, stack trace completo e horário.

---

## 14. Quando escalar para Desenvolvimento

Escalar para **N2/Desenvolvimento** quando:

- O status retornado for `500 Internal Server Error` (erro técnico/inesperado).
- O status retornado for `502 Bad Gateway` (falha na integração com o ViaCEP) **e** o problema persistir mesmo com o CEP correto e o serviço ViaCEP (`https://viacep.com.br`) acessível externamente.
- O mesmo erro de validação (`400`) ocorrer para um payload que, segundo as regras documentadas no Swagger, **deveria** ser válido (possível bug na regra de validação).
- O erro se repetir para múltiplos usuários em um curto intervalo de tempo (possível indisponibilidade de banco de dados, Redis ou da própria API).

**Não escalar** quando o erro for `400` com `fieldErrors` claramente apontando dado inválido informado pelo usuário — nesse caso, o fluxo correto é orientar o usuário (workaround) e registrar a solução na base de conhecimento.
