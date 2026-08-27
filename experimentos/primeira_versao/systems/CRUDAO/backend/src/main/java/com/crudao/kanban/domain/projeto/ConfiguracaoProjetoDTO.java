package com.crudao.kanban.domain.projeto;

/** Toggles configuráveis do projeto — RF-016. */
public record ConfiguracaoProjetoDTO(
    boolean devPodeExcluirTarefa, boolean devPodeEditarTarefaIniciada, boolean gestorVeBoard) {}
