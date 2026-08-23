package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.Workflow;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tarefa — unidade de trabalho movida pelo board (RF-002, RF-003, RF-004, RF-012). */
@Entity
@Table(name = "tarefa")
@Getter
@Setter
@NoArgsConstructor
public class Tarefa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "projeto_id")
  private Projeto projeto;

  /** Herdado do projeto no momento da criação/movimentação entre projetos. */
  @ManyToOne(optional = false)
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;

  @ManyToOne(optional = false)
  @JoinColumn(name = "etapa_atual_id")
  private Etapa etapaAtual;

  @ManyToOne
  @JoinColumn(name = "raia_id")
  private Raia raia;

  @Enumerated(EnumType.STRING)
  private TipoTarefa tipo;

  @NotBlank private String titulo;

  private String descricao;

  private UUID responsavelId;

  /** Estado atual de impedimento — RF-004. Não interfere na movimentação entre etapas. */
  private boolean impedida;

  private Instant criadoEm = Instant.now();

  private Instant atualizadoEm = Instant.now();
}
