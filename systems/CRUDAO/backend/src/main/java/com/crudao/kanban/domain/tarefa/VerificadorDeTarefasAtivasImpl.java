package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Implementação real de {@link VerificadorDeTarefasAtivas} — RN-005 (TASK-02.1). */
@Component
@RequiredArgsConstructor
public class VerificadorDeTarefasAtivasImpl implements VerificadorDeTarefasAtivas {

  private final TarefaRepository tarefaRepository;

  @Override
  public boolean existemTarefasNaEtapa(UUID etapaId) {
    return tarefaRepository.existsByEtapaAtualId(etapaId);
  }

  @Override
  public boolean existemTarefasNoProjeto(UUID projetoId) {
    return tarefaRepository.existsByProjetoId(projetoId);
  }

  @Override
  public boolean existemTarefasNaRaia(UUID raiaId) {
    return tarefaRepository.existsByRaiaId(raiaId);
  }
}
