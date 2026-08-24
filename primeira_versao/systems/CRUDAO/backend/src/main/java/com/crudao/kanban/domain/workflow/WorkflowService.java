package com.crudao.kanban.domain.workflow;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.common.VerificadorDeTarefasAtivas;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.security.AutorizacaoProjetoService;
import com.crudao.kanban.security.UsuarioContexto;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkflowService {

  private static final String PERMISSAO = "workflow:gerenciar";

  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final TransicaoRepository transicaoRepository;
  private final WorkflowMapper workflowMapper;
  private final VerificadorDeTarefasAtivas verificadorDeTarefasAtivas;
  private final AutorizacaoProjetoService autorizacaoProjetoService;
  private final UsuarioContexto usuarioContexto;

  @Transactional(readOnly = true)
  public List<WorkflowDTO> listarPorProjeto(UUID projetoId) {
    return workflowRepository.findByProjetoId(projetoId).stream()
        .map(workflowMapper::paraDTO)
        .toList();
  }

  @Transactional
  public WorkflowDTO criar(WorkflowRequest request) {
    exigirPermissao(request.projetoId());
    Workflow workflow = new Workflow();
    workflow.setProjetoId(request.projetoId());
    workflow.setNome(request.nome());
    return workflowMapper.paraDTO(workflowRepository.save(workflow));
  }

  /** Editar workflow afeta todas as tarefas do projeto (confirmado na entrevista de techspec). */
  @Transactional
  public WorkflowDTO editar(UUID id, WorkflowRequest request) {
    Workflow workflow = buscarEntidade(id);
    exigirPermissao(workflow.getProjetoId());
    workflow.setNome(request.nome());
    workflow.setVersao(workflow.getVersao() + 1);
    return workflowMapper.paraDTO(workflowRepository.save(workflow));
  }

  @Transactional
  public void excluir(UUID id) {
    Workflow workflow = buscarEntidade(id);
    exigirPermissao(workflow.getProjetoId());
    if (verificadorDeTarefasAtivas.existemTarefasNoProjeto(workflow.getProjetoId())) {
      throw new RegraDeNegocioException(
          "Não é possível excluir o workflow '%s': há tarefas ativas no projeto. Migre as tarefas antes."
              .formatted(workflow.getNome()));
    }
    workflowRepository.delete(workflow);
  }

  /**
   * RN-003: toda etapa não-final deve ter ao menos uma transição de saída configurada. Retorna a
   * lista de nomes de etapas em violação (vazia = workflow consistente).
   */
  @Transactional(readOnly = true)
  public List<String> validarConsistencia(UUID workflowId) {
    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflowId);
    List<Transicao> transicoes = transicaoRepository.findByWorkflowId(workflowId);
    return etapas.stream()
        .filter(etapa -> !etapa.isEtapaFinal())
        .filter(
            etapa ->
                transicoes.stream()
                    .noneMatch(t -> t.getEtapaOrigem().getId().equals(etapa.getId())))
        .map(Etapa::getNome)
        .toList();
  }

  private Workflow buscarEntidade(UUID id) {
    return workflowRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Workflow não encontrado: " + id));
  }

  private void exigirPermissao(UUID projetoId) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    autorizacaoProjetoService.exigirPermissao(usuario, projetoId, PERMISSAO);
  }
}
