package com.crudao.kanban.domain.projeto;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Toggles de permissão configuráveis por projeto (RF-016) — conjunto fechado, não é RBAC granular
 * customizável (BDR-001). Criada com os defaults no momento da criação do {@link Projeto}. Uso dos
 * toggles no enforcement é escopo da TASK-01.3; esta task só cria a estrutura.
 */
@Entity
@Table(name = "configuracao_projeto")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracaoProjeto {

  @Id private UUID projetoId;

  /** Default {@code false} (RN-009). */
  private boolean devPodeExcluirTarefa;

  /**
   * Default {@code false} — ignora a trava de "tarefa iniciada" para {@code dev} (RN-009/RN-010).
   */
  private boolean devPodeEditarTarefaIniciada;

  /** Default {@code false} — {@code gestor} ganha leitura do board além do dashboard (RN-013). */
  private boolean gestorVeBoard;
}
