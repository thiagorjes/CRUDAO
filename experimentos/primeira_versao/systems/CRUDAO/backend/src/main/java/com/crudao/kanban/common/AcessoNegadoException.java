package com.crudao.kanban.common;

/**
 * Usuário autenticado, mas sem a permissão necessária para a operação (RNF-003) — mapeada para HTTP
 * 403 pelo {@link ApiExceptionHandler}.
 */
public class AcessoNegadoException extends RuntimeException {
  public AcessoNegadoException(String message) {
    super(message);
  }
}
