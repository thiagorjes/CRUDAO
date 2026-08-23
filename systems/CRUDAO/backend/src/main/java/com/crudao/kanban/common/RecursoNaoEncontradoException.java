package com.crudao.kanban.common;

/** Entidade não encontrada por id — mapeada para HTTP 404 pelo {@link ApiExceptionHandler}. */
public class RecursoNaoEncontradoException extends RuntimeException {
  public RecursoNaoEncontradoException(String message) {
    super(message);
  }
}
