package com.crudao.kanban.common;

/**
 * Violação de regra de negócio (RN-*) do PRD — mapeada para HTTP 409 pelo {@link
 * ApiExceptionHandler}.
 */
public class RegraDeNegocioException extends RuntimeException {
  public RegraDeNegocioException(String message) {
    super(message);
  }
}
