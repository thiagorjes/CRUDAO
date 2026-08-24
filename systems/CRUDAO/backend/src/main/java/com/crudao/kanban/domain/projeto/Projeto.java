package com.crudao.kanban.domain.projeto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Projeto — RF-008. Agrupa workflow(s), raias e tarefas. */
@Entity
@jakarta.persistence.Table(name = "projeto")
@Getter
@Setter
@NoArgsConstructor
public class Projeto {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank private String nome;

  private String descricao;

  /** Workflow atualmente em uso pelo projeto (RF-009 — pode ser trocado/editado). */
  private UUID workflowAtivoId;

  /** Preenchida = projeto somente leitura para todos os papéis (RN-015, RF-008). */
  private Instant dataFinalizacao;

  private Instant criadoEm = Instant.now();

  private Instant atualizadoEm = Instant.now();
}
