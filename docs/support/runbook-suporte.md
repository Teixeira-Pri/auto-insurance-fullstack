# Runbook de Suporte — Auto Insurance API

Este runbook descreve o **fluxo prático** que um analista de Suporte/Sustentação deve seguir ao receber um chamado relacionado ao **Auto Insurance Fullstack** (frontend React + API `auto-insurance-api`).

O objetivo é padronizar a investigação, reduzir o tempo de diagnóstico e garantir que as informações corretas cheguem ao time de Desenvolvimento quando a escalada for necessária.

---

## 1. Recebimento do chamado

Ao receber o chamado, registre no mínimo:

- Nome/identificação do solicitante e canal de origem.
- Descrição do problema, na linguagem do usuário.
- Data e horário aproximados da ocorrência.
- Ambiente (desenvolvimento local, homologação, produção).
- Se possível, print de tela e/ou mensagem de erro exibida.

> Use o template de incidente em [`.github/ISSUE_TEMPLATE/incidente_tecnico.md`](../../.github/ISSUE_TEMPLATE/incidente_tecnico.md) para padronizar o registro.

---

## 2. Classificação por impacto e urgência

| Impacto | Descrição | Exemplo |
|---|---|---|
| **Alto** | Funcionalidade principal indisponível para vários usuários | API retornando `500`/`502` para todas as cotações |
| **Médio** | Funcionalidade degradada (lenta) ou erro pontual recorrente | Lentidão no cálculo (INC-002) |
| **Baixo** | Erro pontual de um único usuário, com workaround simples | Erro de validação (`400`) por dado inválido (INC-001) |

| Urgência | Descrição |
|---|---|
| **Alta** | Afeta operação em produção, sem workaround |
| **Média** | Afeta um fluxo importante, mas com workaround disponível |
| **Baixa** | Não bloqueia o uso do sistema |

A combinação **Impacto x Urgência** define a prioridade (P1 a P4) e o SLA de resposta, conforme política da equipe.

---

## 3. Coleta de evidências

Sempre que possível, colete:

- [ ] Payload completo da requisição (JSON enviado para `POST /api/v1/insurance/calculate`).
- [ ] Resposta completa (status HTTP + corpo JSON).
- [ ] Print da tela (frontend) com a mensagem de erro.
- [ ] Horário exato (com fuso horário) da ocorrência.
- [ ] CPF/CEP/placa envolvidos (sempre **mascarados** ao registrar o chamado, ex.: `123.***.***-09`).

> Esses dados são essenciais tanto para localizar o `traceId` nos logs quanto para reproduzir o cenário em ambiente de desenvolvimento.

---

## 4. Validação de endpoint/API

Antes de aprofundar a investigação, confirme que a API está no ar e respondendo:

```bash
# Health check
curl http://localhost:8080/api/v1/insurance/health

# Resposta esperada:
# {"status":"UP","service":"auto-insurance-api","version":"1.0.0"}

# Health check completo (Actuator, com detalhes de DB/Redis)
curl http://localhost:8080/actuator/health
```

Reproduza a chamada que o usuário fez, usando o **mesmo payload** coletado na etapa anterior:

```bash
curl -i -X POST http://localhost:8080/api/v1/insurance/calculate \
  -H "Content-Type: application/json" \
  -d @payload.json
```

Compare o status HTTP e o corpo da resposta com o que o usuário relatou. Consulte o [Swagger UI](http://localhost:8080/swagger-ui.html) (`/api-docs` para o JSON OpenAPI) para confirmar o contrato esperado do endpoint.

---

## 5. Verificação de logs

Os logs da aplicação seguem o padrão configurado em `application.properties`:

```properties
logging.level.com.insurance.auto=DEBUG
logging.pattern.level=%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]
```

Ou seja, cada linha inclui `[auto-insurance-api,<traceId>,<spanId>]`. Procure por:

- `Solicitação de cálculo recebida para CPF: ...` — confirma que a requisição chegou ao controller.
- `Cálculo concluído. Prêmio total: ... | Perfil: ...` — confirma que o cálculo terminou com sucesso.
- `WARN ... ViaCEP indisponível ...` — indica fallback de localização.
- `ERROR ... Erro inesperado: ...` — indica falha tratada pelo `GlobalExceptionHandler` (HTTP 500).

Filtre os logs pelo horário aproximado do chamado e pelo CPF mascarado para localizar a requisição correta.

---

## 6. Uso do Trace ID

1. A partir do log localizado na etapa anterior, copie o valor de `traceId` (ex.: `6f8a1b2c3d4e5f6071829304a1b2c3d`).
2. Use esse `traceId` para filtrar **todas** as linhas de log relacionadas àquela requisição específica — reconstruindo o fluxo completo (recebimento → consulta ViaCEP → cálculo → resposta).
3. Se o Zipkin estiver disponível (`http://localhost:9411`), busque pelo mesmo `traceId` para visualizar a linha do tempo (spans) da requisição, incluindo tempos de cada etapa.

> Consulte [incident-001](incident-001-error-calculo-cotacao.md) para um exemplo passo a passo completo de uso do Trace ID.

---

## 7. Consulta ao banco de dados

A API utiliza PostgreSQL (`insurance_db`), com duas tabelas mapeadas via JPA: `drivers` e `vehicles`. **O resultado do cálculo de cotação não é persistido** — apenas dados cadastrais de condutor/veículo (quando registrados).

Use as consultas de [sql-consultas-suporte.md](sql-consultas-suporte.md) para:

- Buscar um condutor por CPF/ID.
- Buscar um veículo por placa/ID.
- Conferir os dados que alimentariam o cálculo (idade, tempo de CNH, histórico de sinistros, idade do veículo).
- Validar se um CPF/placa informado em um chamado corresponde a um cadastro existente.

---

## 8. Verificação de cache Redis

O Redis é usado **apenas** para cache das respostas do ViaCEP (`@Cacheable(value = "viacep", ...)`, TTL 24h).

```bash
# Redis está no ar?
redis-cli ping

# Chaves de cache de CEP existentes
redis-cli KEYS "viacep::*"

# Conteúdo e TTL de uma chave específica
redis-cli GET "viacep::01310100"
redis-cli TTL "viacep::01310100"
```

Use esta verificação principalmente em chamados de **lentidão** (ver [incident-002](incident-002-lentidao-api.md)).

---

## 9. Comunicação com o usuário

- Use **linguagem simples**, sem termos técnicos internos (não citar `traceId`, `Bucket4j`, nomes de classes, etc. diretamente ao usuário final).
- Se o problema for um **erro de validação** (ex.: CPF, CEP ou valor do veículo inválidos), explique **qual campo** precisa ser corrigido, usando a mensagem amigável já retornada em `fieldErrors[].message`.
- Se o problema exigir investigação adicional, informe um prazo estimado de retorno, alinhado ao SLA definido na etapa 2.
- Ao concluir, confirme com o usuário se o problema foi resolvido antes de encerrar o chamado.

---

## 10. Escalonamento para N2/N3 ou Desenvolvimento

Escale quando:

- O erro for `500 Internal Server Error` ou `502 Bad Gateway` persistente.
- Houver divergência entre o comportamento observado e a regra de negócio documentada (ver [incident-003](incident-003-classificacao-risco-incorreta.md)).
- O problema afetar múltiplos usuários simultaneamente (possível indisponibilidade de infraestrutura — Redis, PostgreSQL, ou da própria API).
- A causa raiz não puder ser confirmada apenas com logs, banco de dados e cache (necessário acesso ao código/ambiente de desenvolvimento).

**Pacote mínimo para escalonamento:**

1. Descrição do problema e impacto.
2. Payload(s) e resposta(s) completos.
3. `traceId`(s) relevantes.
4. Trechos de log relevantes (com timestamps).
5. Resultado das verificações de banco de dados e Redis (se aplicável).
6. Hipótese de causa raiz já investigada pelo suporte (mesmo que não confirmada).

---

## 11. Registro da solução na base de conhecimento

Ao final de cada chamado:

1. Verifique se a pergunta/cenário já existe em [base-conhecimento.md](base-conhecimento.md).
2. Se for um cenário novo e recorrente, adicione uma nova entrada no formato FAQ (pergunta + resposta objetiva + referência ao incidente, se houver).
3. Se a solução envolveu uma consulta SQL nova e útil, adicione-a em [sql-consultas-suporte.md](sql-consultas-suporte.md).
4. Se o chamado revelou uma melhoria possível (ex.: expor `traceId` na resposta da API), registre como sugestão na seção correspondente do incidente e, se aplicável, comunique ao time de Produto.

Manter a base de conhecimento atualizada reduz o tempo de resolução de chamados futuros semelhantes.
