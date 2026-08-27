package com.crudao.kanban.domain.tarefa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Observador de uma Tarefa — usuário cadastrado que recebe notificação a cada transição de etapa
 * (RF-005, RN-007). Referencia o usuário apenas por ID: a entidade Usuário/RBAC é escopo da
 * TASK-04.1.
 */
@Entity
@Table(
    name = "observador",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tarefa_id", "usuario_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Observador {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "tarefa_id")
  private Tarefa tarefa;

  private UUID usuarioId;
}
