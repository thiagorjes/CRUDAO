package com.crudao.kanban.domain.papel;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelPermissaoAuditoriaRepository
        extends JpaRepository<PapelPermissaoAuditoria, UUID> {}
