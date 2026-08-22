package com.crudao.kanban.domain.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Workflow de um projeto — RF-009. Define as etapas e transições permitidas. */
@Entity
@Table(name = "workflow")
@Getter
@Setter
@NoArgsConstructor
public class Workflow {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull private UUID projetoId;

  @NotBlank private String nome;

  /** Incrementada a cada edição relevante — rastreabilidade de mudanças de fluxo. */
  private int versao = 1;
}
