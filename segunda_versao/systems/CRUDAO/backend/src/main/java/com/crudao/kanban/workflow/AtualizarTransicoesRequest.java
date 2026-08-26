package com.crudao.kanban.workflow;

import java.util.List;
import java.util.UUID;

public record AtualizarTransicoesRequest(List<UUID> etapasDestinoIds) {}
