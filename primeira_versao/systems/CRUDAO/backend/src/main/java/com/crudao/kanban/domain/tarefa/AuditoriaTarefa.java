package com.crudao.kanban.domain.tarefa;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Histórico de auditoria de uma {@link Tarefa} (RF-017). Gravado no mesmo Service e na mesma
 * transação da alteração que originou o registro (troca de responsável, edição de título/descrição,
 * movimentação de etapa) — nunca em processo assíncrono separado. Somente leitura via API, sem
 * endpoint de escrita direta.
 */
@Entity
@Table(
    name = "auditoria_tarefa",
    indexes = @Index(name = "idx_auditoria_tarefa", columnList = "tarefa_id, criado_em"))
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaTarefa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "tarefa_id")
  private Tarefa tarefa;

  private UUID usuarioId;

  @Enumerated(EnumType.STRING)
  private CampoAuditoria campo;

  private String valorAnterior;

  private String valorNovo;

  private Instant criadoEm = Instant.now();
}
