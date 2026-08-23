package com.crudao.kanban.domain.tarefa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Criação de tarefa: exige projeto e etapa inicial. Edição ignora {@code projetoId}/{@code
 * etapaInicialId} (usar os endpoints de movimentação para isso).
 */
public record TarefaRequest(
    @NotNull UUID projetoId,
    UUID etapaInicialId,
    UUID raiaId,
    @NotNull TipoTarefa tipo,
    @NotBlank String titulo,
    String descricao,
    UUID responsavelId) {}
