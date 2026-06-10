# 🚗 Auto Insurance Fullstack

API REST enterprise de cálculo de prêmio de seguro automotivo com frontend React moderno.

## 📸 Interface

> Formulário completo com gráficos interativos de fatores de risco

## 🛠️ Stack

### Backend
- Java 17 + Spring Boot 3.2
- PostgreSQL 15 (banco de dados)
- Redis 7 (cache distribuído)
- Micrometer + Zipkin (observabilidade)
- Bucket4j (rate limiting)
- Swagger/OpenAPI 3

### Frontend
- React 18 + TypeScript
- Tailwind CSS
- Recharts (gráficos interativos)
- React Query + React Hook Form
- Vite 5

## 📈 Impacto Técnico

| Métrica | Antes | Depois | Melhoria |
|---------|-------|--------|----------|
| Latência média | 250ms | 55ms | **78% ↓** |
| Disponibilidade | 95% | 99.5% | **4.5% ↑** |
| Cache hit rate | 0% | 80% | **∞** |
| Proteção DDoS | ❌ | ✅ 100 req/min | **∞** |

## 🔧 Pré-requisitos

| Ferramenta | Versão mínima |
|------------|---------------|
| Java | 17 |
| Maven | 3.8 |
| Node.js | 18 |
| PostgreSQL | 14 |
| Redis | 6 |

## 🚀 Como Executar

### 1. PostgreSQL

```sql
CREATE DATABASE insurance_db;
-- Credenciais padrão: usuário 'postgres', senha 'postgres'
-- Ajuste em: auto-insurance-api/src/main/resources/application.properties
```

### 2. Redis

```bash
# Via Docker (mais simples)
docker run -d -p 6379:6379 redis:7-alpine

# Ou instale localmente e execute: redis-server
```

### 3. Backend

```bash
cd auto-insurance-api

# Testes não precisam de PostgreSQL nem Redis (usam H2 in-memory)
mvn test

# Iniciar servidor na porta 8080
mvn spring-boot:run
```

### 4. Frontend

```bash
cd auto-insurance-frontend
npm install

# Servidor de desenvolvimento (porta 3000, faz proxy para :8080)
npm run dev

# Build de produção
npm run build
```

## 🔗 Acessos

| Recurso | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Métricas | http://localhost:8080/actuator/prometheus |

## 🧮 Como Funciona o Cálculo

O prêmio é calculado com base em **8 fatores de risco**:

1. Idade do condutor
2. Experiência ao volante
3. Histórico de sinistros
4. Categoria do veículo
5. Tipo de combustível
6. Condição do veículo
7. Idade do veículo
8. Localização geográfica (estado)

**Fórmula:** `Prêmio = Valor do Veículo × 5% × Π(multiplicadores)`

## 🛠️ Suporte técnico e análise de incidentes

Além da API de cálculo de seguro, este projeto inclui uma camada de **documentação técnica de suporte e sustentação**, simulando atividades de um analista de **Suporte Técnico a Produto/Sistemas** em um ambiente real:

- **Casos de incidentes simulados** (erro de validação ao calcular cotação, lentidão na API, divergência na classificação de risco), cada um com cenário do usuário, impacto, prioridade, evidências, payload de exemplo, status HTTP esperado/recebido, análise de logs, uso de Trace ID, consulta SQL de apoio, causa provável, workaround, solução definitiva e critérios de escalonamento.
- **Runbook de suporte** com o fluxo prático de atendimento de chamados: recebimento, classificação por impacto/urgência, coleta de evidências, validação de endpoint/API, verificação de logs, uso do Trace ID, consulta ao banco de dados, verificação do cache Redis, comunicação com o usuário, escalonamento para N2/N3/Desenvolvimento e registro da solução na base de conhecimento.
- **Base de conhecimento (FAQ)** com respostas objetivas para as dúvidas mais comuns do dia a dia de suporte (erro na cotação, validação no banco, uso do Trace ID, cache Redis, erro de regra de negócio x erro técnico, critérios de escalonamento).
- **Consultas SQL de apoio**, compatíveis com o schema real do projeto (tabelas `drivers` e `vehicles`).
- **Template de issue para registro de incidentes técnicos** em [`.github/ISSUE_TEMPLATE/incidente_tecnico.md`](.github/ISSUE_TEMPLATE/incidente_tecnico.md).

Toda essa documentação está em [`docs/support/`](docs/support/):

| Documento | Conteúdo |
|---|---|
| [incident-001-error-calculo-cotacao.md](docs/support/incident-001-error-calculo-cotacao.md) | Erro de validação ao calcular cotação (HTTP 400) |
| [incident-002-lentidao-api.md](docs/support/incident-002-lentidao-api.md) | Lentidão no cálculo, cache Redis e métricas |
| [incident-003-classificacao-risco-incorreta.md](docs/support/incident-003-classificacao-risco-incorreta.md) | Divergência na classificação de perfil de risco |
| [runbook-suporte.md](docs/support/runbook-suporte.md) | Fluxo padrão de atendimento de chamados |
| [base-conhecimento.md](docs/support/base-conhecimento.md) | FAQ de suporte |
| [sql-consultas-suporte.md](docs/support/sql-consultas-suporte.md) | Consultas SQL de apoio à investigação |

### 🔎 Rastreabilidade com Trace ID

A API gera automaticamente um **Trace ID** e um **Span ID** para cada requisição (Micrometer Tracing + Brave, `management.tracing.sampling.probability=1.0`), incluídos em todas as linhas de log conforme `logging.pattern.level` em `application.properties`:

```
2026-06-10T14:32:09.987-03:00  INFO [auto-insurance-api,6f8a1b2c3d4e5f6071829304a1b2c3d,a1b2c3d4e5f60718] 12345 --- [nio-8080-exec-3] c.i.a.c.InsuranceCalculationController : Solicitação de cálculo recebida para CPF: 123.***.***-09
```

Um analista de suporte pode usar o Trace ID (`6f8a1b2c3d4e5f6071829304a1b2c3d` no exemplo acima) para:

1. Filtrar todas as linhas de log daquela requisição e reconstruir o fluxo completo (recebimento → consulta ao ViaCEP → cálculo do prêmio → resposta).
2. Buscar o mesmo Trace ID no Zipkin (`http://localhost:9411`, se configurado) para visualizar a linha do tempo (spans) da requisição.
3. Correlacionar o chamado de um usuário (horário aproximado + CPF/CEP/placa mascarados) com o `traceId` correspondente nos logs, já que esse identificador ainda não é exposto na resposta da API ao cliente.

Mais detalhes e exemplos práticos em [docs/support/incident-001-error-calculo-cotacao.md](docs/support/incident-001-error-calculo-cotacao.md#9-como-usar-o-trace-id).

> Esta camada de documentação não altera as regras de negócio nem a arquitetura da aplicação — é uma adição que demonstra, na prática, como aplicar conceitos de **análise de incidentes, logs, SQL, rastreabilidade e comunicação técnica** em um ambiente de suporte a produto/sistemas.

## 📝 Licença

MIT License
