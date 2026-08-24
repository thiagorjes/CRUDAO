# Observability — CRUDAO
_Versão: 1.0 | Data: 2026-08-22_

## Logs

- Padrão de log tradicional Linux: arquivo de log local com rotação a cada 5MB.
- Retenção: manter apenas os 10 arquivos de log mais recentes (rotate + purge automático dos mais antigos).
- Sem formato estruturado (JSON) definido nesta fase — texto padrão da aplicação.

## Métricas e tracing

- Sem telemetria (APM/tracing/OpenTelemetry) nesta fase.

## Observação

Esta é uma configuração mínima para a primeira entrega. Reavaliar necessidade de stack de observabilidade mais robusta (ex.: métricas, tracing distribuído) conforme o sistema evoluir e escalar (ver RNF-002 e ADR-002).
