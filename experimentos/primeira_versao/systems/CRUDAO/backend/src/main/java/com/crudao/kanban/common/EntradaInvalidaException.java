package com.crudao.kanban.common;

/**
 * Entrada sintaticamente válida mas semanticamente rejeitada pela regra de negócio (ex.: tentativa
 * de escalação de privilégio em RF-015/G-RBAC-07) — mapeada para HTTP 422 pelo {@link
 * ApiExceptionHandler}.
 */
public class EntradaInvalidaException extends RuntimeException {
  public EntradaInvalidaException(String message) {
    super(message);
  }
}
