package com.crudao.kanban.projeto;

import com.crudao.kanban.domain.usuario.Projeto;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjetoResponse(
        UUID id, String nome, String descricao, Projeto.Status status, OffsetDateTime finalizadoEm) {}
