package com.crudao.kanban.domain.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Etapa (coluna) de um {@link Workflow} — RF-010. */
@Entity
@Table(name = "etapa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Etapa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "workflow_id")
  private Workflow workflow;

  @NotBlank private String nome;

  private int ordem;

  /** Etapa terminal: não possui transição de saída padrão (RN-004), apenas reabertura (RF-012). */
  private boolean etapaFinal;
}
