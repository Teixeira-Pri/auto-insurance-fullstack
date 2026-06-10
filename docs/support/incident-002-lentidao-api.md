# Incidente 002 — Lentidão no cálculo de cotação

| Campo | Valor |
|---|---|
| **ID do incidente** | INC-002 |
| **Categoria** | Performance / Lentidão |
| **Componente afetado** | `auto-insurance-api` — `POST /api/v1/insurance/calculate`, integração ViaCEP, cache Redis |
| **Severidade sugerida** | Média a Alta, dependendo do tempo de resposta observado |

> Caso simulado, baseado na arquitetura real do projeto: cache Redis para respostas do ViaCEP (`ViaCepClient`, `RedisCacheConfig`), métricas Micrometer (`insurance.calculation.duration`, `insurance.calculations.total`, `insurance.viacep.failures`) e Actuator/Prometheus.

---

## 1. Cenário do usuário

Usuários relatam que o botão **"Calcular Prêmio"** demora vários segundos para retornar o resultado, em alguns casos chegando a parecer "travado". O chamado costuma vir descrito como:

> "A cotação está demorando muito para aparecer, às vezes mais de 5 segundos."

---

## 2. Impacto

- Experiência do usuário degradada — em formulários web, atrasos acima de ~1-2 segundos aumentam a taxa de abandono.
- Se a lentidão for generalizada (todas as requisições), pode indicar problema de infraestrutura (Redis, banco de dados, rede até o ViaCEP).
- Se a lentidão ocorrer apenas em **CEPs novos** (primeira consulta), é esperado um tempo maior nessa primeira chamada — o que deve ser diferenciado de um problema real.

---

## 3. Como verificar o tempo de resposta

### a) Pelo navegador (DevTools)
- Aba **Network** → coluna **Time** da requisição `POST /api/v1/insurance/calculate`.

### b) Pelo Swagger UI
- `application.properties` tem `springdoc.swagger-ui.display-request-duration=true`, ou seja, o Swagger UI (`http://localhost:8080/swagger-ui.html`) exibe a duração de cada chamada feita via "Try it out".

### c) Via linha de comando
```bash
curl -w "\nTempo total: %{time_total}s\n" -o /dev/null -s \
  -X POST http://localhost:8080/api/v1/insurance/calculate \
  -H "Content-Type: application/json" \
  -d @payload.json
```

### d) Via métricas do Actuator (Micrometer)
A aplicação expõe um timer customizado **`insurance.calculation.duration`**, registrado em `InsuranceCalculationServiceImpl`, que mede exclusivamente o tempo do cálculo (sem incluir overhead de rede/HTTP):

```bash
curl http://localhost:8080/actuator/metrics/insurance.calculation.duration
```

E o tempo total da requisição HTTP (inclui validação, interceptors, etc.):

```bash
curl http://localhost:8080/actuator/metrics/http.server.requests?tag=uri:/api/v1/insurance/calculate
```

Se exposto em Prometheus, é possível visualizar percentis (P50, P95, P99) — ver `management.metrics.distribution.percentiles-histogram.http.server.requests=true`.

---

## 4. Como validar uso do Redis Cache

A única chamada externa do fluxo de cálculo é ao **ViaCEP** (`ViaCepClient.getAddressByZipCode`), anotada com `@Cacheable(value = "viacep", key = "#zipCode")` e TTL de 24h (`RedisCacheConfig`).

### a) Verificar se o Redis está no ar
```bash
redis-cli ping
# Esperado: PONG
```

### b) Verificar se a chave do CEP está em cache
O Spring Cache armazena as chaves no formato `<nome-do-cache>::<chave>`. Para o CEP `01310100`:

```bash
redis-cli KEYS "viacep::*"
redis-cli GET "viacep::01310100"
redis-cli TTL "viacep::01310100"
```

- Se a chave **existir**, a próxima requisição com o mesmo CEP deve responder rapidamente (cache hit), sem chamar a API externa do ViaCEP.
- Se a chave **não existir** (cache miss), a aplicação chamará `https://viacep.com.br/ws/{cep}/json/` — chamada externa que naturalmente adiciona latência de rede.

### c) Verificar métricas de cache hit/miss
```bash
curl "http://localhost:8080/actuator/metrics/cache.gets?tag=cache:viacep&tag=result:hit"
curl "http://localhost:8080/actuator/metrics/cache.gets?tag=cache:viacep&tag=result:miss"
```

### d) Verificar contador de falhas do ViaCEP
A aplicação registra um contador customizado para falhas na integração:

```bash
curl http://localhost:8080/actuator/metrics/insurance.viacep.failures
```

Um valor crescente indica que o ViaCEP está indisponível/lento e a aplicação está caindo no fallback (`getStateFromCep` retorna `null`, usando multiplicador padrão `1.0` — ver `InsuranceCalculationServiceImpl`).

---

## 5. Possíveis causas

| Causa | Como identificar |
|---|---|
| Redis indisponível ou inacessível | `redis-cli ping` falha; logs podem mostrar exceções de conexão Redis |
| Cache "frio" — muitos CEPs diferentes sendo consultados pela primeira vez | `cache.gets` com `result:miss` alto; `KEYS "viacep::*"` retorna poucas chaves |
| ViaCEP externo lento ou instável | `insurance.viacep.failures` crescendo; logs com `WARN ... ViaCEP indisponível` |
| Banco de dados PostgreSQL lento (pool de conexões esgotado) | métricas de `hikaricp` no Actuator; `org.hibernate.SQL` nos logs (DEBUG habilitado) demorando |
| Rate limiting (Bucket4j) gerando retentativas no frontend | resposta `429 Too Many Requests`, headers `X-Rate-Limit-Remaining` próximos de `0` |
| Volume real de requisições acima da capacidade da instância | `insurance.calculations.total` crescendo rapidamente; CPU/memória da JVM no limite (`/actuator/metrics/jvm.memory.used`) |

---

## 6. Evidências em logs

Com `logging.level.com.insurance.auto=DEBUG`, o fluxo de cálculo gera logs detalhados, incluindo `traceId`/`spanId`:

```
2026-06-10T15:05:01.100-03:00  INFO [auto-insurance-api,9a8b7c6d5e4f30211f0e0d0c0b0a0908,1f0e0d0c0b0a0908] 12345 --- [nio-8080-exec-5] c.i.a.c.InsuranceCalculationController : Solicitação de cálculo recebida para CPF: 123.***.***-09
2026-06-10T15:05:01.105-03:00  DEBUG [auto-insurance-api,9a8b7c6d5e4f30211f0e0d0c0b0a0908,1f0e0d0c0b0a0908] 12345 --- [nio-8080-exec-5] c.i.a.client.ViaCepClient : Consultando ViaCEP para CEP: 01310100
2026-06-10T15:05:01.890-03:00  DEBUG [auto-insurance-api,9a8b7c6d5e4f30211f0e0d0c0b0a0908,1f0e0d0c0b0a0908] 12345 --- [nio-8080-exec-5] c.i.a.client.ViaCepClient : ViaCEP retornou estado: SP para CEP: 01310100
2026-06-10T15:05:01.895-03:00  INFO [auto-insurance-api,9a8b7c6d5e4f30211f0e0d0c0b0a0908,1f0e0d0c0b0a0908] 12345 --- [nio-8080-exec-5] c.i.a.s.i.InsuranceCalculationServiceImpl : Cálculo concluído. Prêmio total: R$ 5865.75 | Perfil: MÉDIO
```

Pontos de atenção ao analisar os timestamps acima (formato `HH:mm:ss.SSS`):

- **Diferença entre o log "Consultando ViaCEP" e "ViaCEP retornou estado"** (neste exemplo, ~785ms): se essa diferença for consistentemente alta, é forte indício de **cache miss + ViaCEP externo lento**.
- Se essa diferença for consistentemente **baixa (poucos ms)**, o cache Redis está funcionando — a lentidão deve estar em outro ponto (banco de dados, rede, JVM).
- Quando o ViaCEP falha, aparece um `WARN`:
  ```
  WARN [auto-insurance-api,...] c.i.a.s.i.InsuranceCalculationServiceImpl : ViaCEP indisponível para CEP 01310100. Usando multiplicador padrão.
  ```

---

## 7. Consulta SQL de apoio

A lentidão pode estar relacionada ao banco de dados (pool de conexões, locks, etc.), mesmo que o endpoint de cálculo não persista os dados da cotação. Para verificar a saúde geral do banco `insurance_db`:

```sql
-- Conexões ativas no banco (PostgreSQL)
SELECT pid, usename, application_name, state, query, query_start
FROM pg_stat_activity
WHERE datname = 'insurance_db'
ORDER BY query_start ASC;

-- Quantidade de registros nas tabelas do domínio (volume de dados)
SELECT
  (SELECT COUNT(*) FROM drivers)  AS total_drivers,
  (SELECT COUNT(*) FROM vehicles) AS total_vehicles;
```

> Mais consultas de apoio em [sql-consultas-suporte.md](sql-consultas-suporte.md).

---

## 8. Ação recomendada

1. Confirmar se a lentidão é **pontual** (primeira consulta a um CEP novo) ou **recorrente** (mesmo CEP, várias vezes).
2. Validar Redis (`redis-cli ping`, `KEYS "viacep::*"`) — se Redis estiver fora do ar, reiniciar o serviço/container (`docker run -d -p 6379:6379 redis:7-alpine` conforme README) e reexecutar o teste.
3. Validar métricas `insurance.viacep.failures` e `cache.gets` para confirmar se o gargalo é a integração externa.
4. Validar `insurance.calculation.duration` para isolar se o tempo está no cálculo em si (CPU/regras) ou na espera por I/O externo (Redis/ViaCEP/DB).
5. Se o problema for de capacidade (muitas requisições simultâneas), verificar o rate limiting (`rate.limit.requests-per-minute=100`) e métricas de JVM/CPU.
6. Documentar o `traceId` da requisição lenta para análise detalhada pelo time de Desenvolvimento, se necessário.

---

## 9. Sugestão de melhoria

- **Expor o `traceId` em um header de resposta** (ex.: `X-Trace-Id`), facilitando a correlação imediata entre o chamado do usuário e os logs/Zipkin.
- **Dashboard dedicado** no Grafana com `insurance.calculation.duration`, `cache.gets` (hit/miss) e `insurance.viacep.failures`, para identificar lentidão proativamente (antes da abertura de chamados).
- **Alerta automático** quando `insurance.viacep.failures` crescer acima de um limiar em curto período, indicando degradação da integração externa.
- Avaliar **pré-aquecimento de cache** (cache warming) para CEPs mais frequentes, reduzindo a taxa de cache miss em horários de pico.
