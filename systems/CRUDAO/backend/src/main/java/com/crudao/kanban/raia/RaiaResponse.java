package com.crudao.kanban.raia;

import java.util.UUID;

public record RaiaResponse(UUID id, String nome, int ordem, boolean global) {}
