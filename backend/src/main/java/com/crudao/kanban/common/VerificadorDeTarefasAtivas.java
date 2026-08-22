package com.crudao.kanban.common;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Porta usada por Projeto/Workflow/Etapa/Raia para aplicar RN-005 (bloquear exclusão se houver
 * tarefas ativas vinculadas).
 *
 * <p><b>Implementação provisória:</b> a entidade Tarefa só é criada na TASK-02.1. Até lá, esta
 * implementação sempre responde "sem tarefas ativas" — a TASK-02.1 deve substituir este bean por
 * uma implementação real consultando o repositório de Tarefa.
 */
@Component
public class VerificadorDeTarefasAtivas {

  public boolean existemTarefasNaEtapa(UUID etapaId) {
    return false; // TODO(TASK-02.1): consultar TarefaRepository.existsByEtapaId(etapaId)
  }

  public boolean existemTarefasNoProjeto(UUID projetoId) {
    return false; // TODO(TASK-02.1): consultar TarefaRepository.existsByProjetoId(projetoId)
  }
}
