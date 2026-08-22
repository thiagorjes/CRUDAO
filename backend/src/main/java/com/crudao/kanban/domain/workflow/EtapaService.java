package com.crudao.kanban.domain.workflow;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EtapaService {

  private final EtapaRepository etapaRepository;
  private final WorkflowRepository workflowRepository;
  private final WorkflowMapper workflowMapper;
  private final VerificadorDeTarefasAtivas verificadorDeTarefasAtivas;

  @Transactional(readOnly = true)
  public List<EtapaDTO> listarPorWorkflow(UUID workflowId) {
    return etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId).stream()
        .map(workflowMapper::paraDTO)
        .toList();
  }

  @Transactional
  public EtapaDTO criar(EtapaRequest request) {
    Workflow workflow =
        workflowRepository
            .findById(request.workflowId())
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Workflow não encontrado: " + request.workflowId()));
    Etapa etapa = new Etapa();
    etapa.setWorkflow(workflow);
    etapa.setNome(request.nome());
    etapa.setOrdem(request.ordem());
    etapa.setEtapaFinal(request.etapaFinal());
    return workflowMapper.paraDTO(etapaRepository.save(etapa));
  }

  @Transactional
  public EtapaDTO editar(UUID id, EtapaRequest request) {
    Etapa etapa = buscarEntidade(id);
    etapa.setNome(request.nome());
    etapa.setOrdem(request.ordem());
    etapa.setEtapaFinal(request.etapaFinal());
    return workflowMapper.paraDTO(etapaRepository.save(etapa));
  }

  /** RN-005: exclusão bloqueada se houver tarefas na etapa. */
  @Transactional
  public void excluir(UUID id) {
    Etapa etapa = buscarEntidade(id);
    if (verificadorDeTarefasAtivas.existemTarefasNaEtapa(id)) {
      throw new RegraDeNegocioException(
          "Não é possível excluir a etapa '%s': há tarefas nela. Migre as tarefas antes."
              .formatted(etapa.getNome()));
    }
    etapaRepository.delete(etapa);
  }

  private Etapa buscarEntidade(UUID id) {
    return etapaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Etapa não encontrada: " + id));
  }
}
