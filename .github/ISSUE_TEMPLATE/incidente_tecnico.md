---
name: Incidente técnico
about: Reportar um problema técnico identificado na Auto Insurance API/Frontend (erro, lentidão, divergência de dados, etc.)
title: "[INCIDENTE] "
labels: incidente, suporte
assignees: ''
---

## Descrição do problema

<!-- Descreva, de forma objetiva, o que está acontecendo. -->

## Ambiente

- [ ] Desenvolvimento local
- [ ] Homologação
- [ ] Produção

**Versão/branch:**
**Data e horário da ocorrência:**

## Endpoint

<!-- Ex.: POST /api/v1/insurance/calculate -->

## Payload

<!-- Cole o JSON enviado na requisição. Mascare dados sensíveis (CPF, e-mail, telefone). -->

```json

```

## Resultado esperado

<!-- O que deveria acontecer (ex.: status 200 com a cotação calculada). -->

## Resultado obtido

<!-- O que de fato aconteceu (status HTTP, corpo da resposta de erro, mensagem exibida no frontend, etc.). -->

```json

```

## Evidências

<!-- Prints de tela, trechos de log, métricas (Actuator/Prometheus), etc. -->

## Trace ID

<!-- traceId/spanId localizados nos logs (formato: [auto-insurance-api,<traceId>,<spanId>]).
     Caso não tenha sido possível localizar, descreva o horário aproximado e o CPF/CEP/placa (mascarados) usados. -->

## Impacto

- [ ] Alto — funcionalidade principal indisponível para vários usuários
- [ ] Médio — funcionalidade degradada ou erro pontual recorrente
- [ ] Baixo — erro pontual, com workaround disponível

## Prioridade

- [ ] P1 - Crítica
- [ ] P2 - Alta
- [ ] P3 - Média
- [ ] P4 - Baixa

## Critérios para encerramento

<!-- O que precisa ser verdadeiro para considerar este incidente resolvido?
     Ex.: a requisição com o payload acima retorna status 200 com o resultado esperado;
     o cálculo retorna o riskProfile correto conforme a regra de negócio documentada. -->

- [ ]
- [ ]
