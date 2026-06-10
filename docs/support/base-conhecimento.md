# Base de Conhecimento — Suporte Auto Insurance API

FAQ com as dúvidas mais comuns no atendimento de chamados relacionados ao **Auto Insurance Fullstack**. Cada resposta referencia a documentação detalhada correspondente.

---

### 1. O que fazer quando a cotação retorna erro?

1. Identifique o **status HTTP** retornado por `POST /api/v1/insurance/calculate`:
   - **`400 Bad Request`** → erro de validação de dados de entrada. Verifique o array `fieldErrors` da resposta — cada item indica o campo (`field`), a mensagem (`message`) e o valor rejeitado (`rejectedValue`). Oriente o usuário a corrigir o campo indicado e tentar novamente.
   - **`422 Unprocessable Entity`** → violação de regra de negócio (`BusinessException`).
   - **`429 Too Many Requests`** → limite de requisições excedido (rate limiting, 100 req/min por IP). Oriente o usuário a aguardar e tentar novamente.
   - **`502 Bad Gateway`** → falha na integração com o ViaCEP.
   - **`500 Internal Server Error`** → erro técnico inesperado. Colete `traceId`, logs e payload, e siga para escalonamento.
2. Reproduza o mesmo payload via `curl`/Swagger UI para confirmar se o erro é consistente.
3. Consulte os logs filtrando pelo horário e CPF mascarado para localizar o `traceId` da requisição.
4. Veja o passo a passo completo em [incident-001-error-calculo-cotacao.md](incident-001-error-calculo-cotacao.md).

---

### 2. Como validar se o cálculo foi registrado no banco?

**Importante:** no fluxo atual da API, o endpoint `POST /api/v1/insurance/calculate` é **stateless** — ele calcula o prêmio e retorna o resultado na resposta HTTP, mas **não grava o resultado da cotação** (valores calculados, `riskFactors`, `riskProfile`, `quoteId`) em nenhuma tabela do banco de dados.

O que existe no banco (`insurance_db`, tabelas `drivers` e `vehicles`, mapeadas via JPA) são apenas **dados cadastrais** de condutores e veículos, caso tenham sido persistidos por algum outro fluxo.

Portanto:

- **Não é possível** "validar se o cálculo foi registrado" pesquisando o `quoteId` no banco — ele existe apenas na resposta da API (e, se o cliente/log armazenar, nos logs de aplicação que utilizam essa resposta).
- É possível verificar se o **CPF/placa** usados na simulação correspondem a registros existentes em `drivers`/`vehicles` (ver [sql-consultas-suporte.md](sql-consultas-suporte.md)).
- Para confirmar que um cálculo específico **foi processado** (mesmo sem persistência), a evidência correta são os **logs estruturados com Trace ID** (ver pergunta 3) — eles registram o início e o fim do processamento, incluindo o prêmio total e o perfil de risco calculados.

> Esse comportamento (resultado não persistido) é uma característica da versão atual do projeto e deve ser levado em conta em qualquer investigação de suporte.

---

### 3. Como identificar uma requisição pelo Trace ID?

A API utiliza **Micrometer Tracing + Brave** (`management.tracing.sampling.probability=1.0`), o que faz com que **toda** requisição HTTP gere um `traceId` único, presente em todas as linhas de log daquela requisição:

```
2026-06-10T14:32:09.987-03:00  INFO [auto-insurance-api,6f8a1b2c3d4e5f6071829304a1b2c3d,a1b2c3d4e5f60718] ...
```

Passos:

1. Localize uma linha de log relacionada ao chamado (por horário aproximado + CPF mascarado).
2. Copie o `traceId` (primeiro valor entre colchetes após o nome da aplicação).
3. Filtre todos os logs por esse `traceId` para reconstruir o ciclo completo da requisição.
4. Se disponível, busque o mesmo `traceId` no Zipkin (`http://localhost:9411`) para visualizar a linha do tempo (spans).

> **Atenção:** o `traceId` **não é retornado ao cliente** (frontend) hoje — nem no corpo da resposta, nem em header HTTP. Por isso, a correlação inicial sempre depende de horário + dado mascarado (CPF/CEP/placa). Ver sugestão de melhoria em [incident-001](incident-001-error-calculo-cotacao.md#9-como-usar-o-trace-id) e [incident-002](incident-002-lentidao-api.md#9-sugestão-de-melhoria).

---

### 4. Como verificar se o Redis Cache está funcionando?

O Redis é usado **somente** para cache das respostas do ViaCEP (`@Cacheable(value = "viacep", key = "#zipCode")`, TTL de 24 horas).

```bash
# 1. Redis está no ar?
redis-cli ping
# Esperado: PONG

# 2. Existem chaves de cache de CEP?
redis-cli KEYS "viacep::*"

# 3. Conteúdo e tempo de vida restante de uma chave
redis-cli GET "viacep::01310100"
redis-cli TTL "viacep::01310100"
```

Adicionalmente, métricas do Actuator mostram a taxa de acerto/erro de cache:

```bash
curl "http://localhost:8080/actuator/metrics/cache.gets?tag=cache:viacep&tag=result:hit"
curl "http://localhost:8080/actuator/metrics/cache.gets?tag=cache:viacep&tag=result:miss"
```

Detalhes completos em [incident-002-lentidao-api.md](incident-002-lentidao-api.md#4-como-validar-uso-do-redis-cache).

---

### 5. Como diferenciar erro de regra de negócio e erro técnico?

| Característica | Erro de regra de negócio / validação | Erro técnico |
|---|---|---|
| **Status HTTP** | `400` (validação de campo) ou `422` (regra de negócio) | `500` (erro interno) ou `502` (falha em integração externa) |
| **Corpo da resposta** | `ValidationErrorResponse` com `fieldErrors` detalhando o campo, ou `ErrorResponse` com `error: "Business Rule Violation"` | `ErrorResponse` com `error: "Internal Server Error"` (mensagem genérica) ou `error: "External Service Error"` |
| **Origem** | Dado informado pelo usuário não atende às regras documentadas (CPF inválido, CEP fora do padrão, valor fora da faixa, etc.) | Falha inesperada no processamento (NullPointerException, indisponibilidade de banco/Redis, falha não tratada) |
| **Log correspondente** | `WARN` (ex.: `MethodArgumentNotValidException`, "Regra de negócio violada") | `ERROR` com stack trace completo ("Erro inesperado: ...") |
| **Ação do suporte** | Orientar o usuário a corrigir o dado (workaround imediato) | Coletar evidências e escalar para Desenvolvimento |

Regra prática: **se a resposta tem `fieldErrors` ou status `400`/`422` com mensagem clara em português apontando o campo problemático, é erro de regra de negócio/validação.** Se a resposta tem mensagem genérica ("Ocorreu um erro interno...") e status `500`, é erro técnico.

---

### 6. Quando escalar um chamado para desenvolvimento?

Escalar para **N2/N3 (Desenvolvimento)** quando:

- O status retornado for `500 Internal Server Error` (erro técnico inesperado).
- O status `502 Bad Gateway` persistir mesmo com o ViaCEP acessível externamente e o CEP correto.
- O `totalRiskMultiplier` ou o `riskProfile` retornados **não correspondem** às regras documentadas (ver [incident-003](incident-003-classificacao-risco-incorreta.md)).
- O mesmo erro `400`/`422` ocorrer para um payload que, segundo a documentação (Swagger/OpenAPI), deveria ser válido.
- O problema afeta múltiplos usuários simultaneamente, sugerindo indisponibilidade de infraestrutura (Redis, PostgreSQL, ou da própria API).

**Não escalar** quando:

- O erro for `400` com `fieldErrors` apontando claramente um dado inválido informado pelo usuário (workaround = corrigir o dado).
- O comportamento estiver correto segundo as regras de negócio documentadas, mesmo que o resultado tenha mudado em relação a uma simulação anterior (ex.: mudança de faixa etária por passagem de tempo — comportamento esperado).

Sempre anexar ao chamado escalado: payload, resposta completa, `traceId`, trechos de log relevantes e a hipótese de causa raiz já investigada (ver checklist completo em [runbook-suporte.md](runbook-suporte.md#10-escalonamento-para-n2n3-ou-desenvolvimento)).
