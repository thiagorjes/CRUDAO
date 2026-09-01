package com.crudao.kanban.domain.papel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PapelPermissaoAuditoriaRepository
        extends JpaRepository<PapelPermissaoAuditoria, UUID> {}
