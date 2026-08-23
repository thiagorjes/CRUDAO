package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoEngine;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD e movimentação de Tarefa — RF-002, RF-003, RF-004, RF-012. */
@Service
@RequiredArgsConstructor
public class TarefaService {

  private final TarefaRepository tarefaRepository;
  private final ProjetoRepository projetoRepository;
  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final RaiaRepository raiaRepository;
  private final TransicaoRepository transicaoRepository;
  private final TransicaoEngine transicaoEngine;
  private final TarefaMapper tarefaMapper;

  @Transactional(readOnly = true)
  public List<TarefaDTO> listarPorProjeto(UUID projetoId) {
    return tarefaRepository.findByProjetoIdOrderByCriadoEmAsc(projetoId).stream()
        .map(tarefaMapper::paraDTO)
        .toList();
  }

  @Transactional(readOnly = true)
  public TarefaDTO buscar(UUID id) {
    return tarefaMapper.paraDTO(buscarEntidade(id));
  }

  @Transactional
  public TarefaDTO criar(TarefaRequest request) {
    Projeto projeto = buscarProjeto(request.projetoId());
    Workflow workflow = buscarWorkflowAtivo(projeto);
    Etapa etapaInicial = buscarEtapaDoWorkflow(request.etapaInicialId(), workflow);

    Tarefa tarefa = new Tarefa();
    tarefa.setProjeto(projeto);
    tarefa.setWorkflow(workflow);
    tarefa.setEtapaAtual(etapaInicial);
    tarefa.setRaia(buscarRaiaOuNula(request.raiaId()));
    tarefa.setTipo(request.tipo());
    tarefa.setTitulo(request.titulo());
    tarefa.setDescricao(request.descricao());
    tarefa.setResponsavelId(request.responsavelId());
    return tarefaMapper.paraDTO(tarefaRepository.save(tarefa));
  }

  @Transactional
  public TarefaDTO editar(UUID id, TarefaRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    tarefa.setRaia(buscarRaiaOuNula(request.raiaId()));
    tarefa.setTipo(request.tipo());
    tarefa.setTitulo(request.titulo());
    tarefa.setDescricao(request.descricao());
    tarefa.setResponsavelId(request.responsavelId());
    tarefa.setAtualizadoEm(java.time.Instant.now());
    return tarefaMapper.paraDTO(tarefaRepository.save(tarefa));
  }

  @Transactional
  public void excluir(UUID id) {
    tarefaRepository.delete(buscarEntidade(id));
  }

  /**
   * Move a tarefa para outra etapa do mesmo workflow — RF-002. Só é permitido se houver {@link
   * Transicao} (NORMAL ou REABERTURA, RF-012) ligando a etapa atual à etapa destino. O estado de
   * impedimento não interfere nesta regra (RF-004).
   */
  @Transactional
  public TarefaDTO mover(UUID id, TarefaMoverRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    Etapa etapaDestino = buscarEtapaDoWorkflow(request.etapaDestinoId(), tarefa.getWorkflow());

    List<Transicao> transicoes = transicaoRepository.findByWorkflowId(tarefa.getWorkflow().getId());
    if (!transicaoEngine.transicaoPermitida(tarefa.getEtapaAtual(), etapaDestino, transicoes)) {
      throw new RegraDeNegocioException(
          "Transição não permitida pelo workflow: '%s' -> '%s'"
              .formatted(tarefa.getEtapaAtual().getNome(), etapaDestino.getNome()));
    }

    tarefa.setEtapaAtual(etapaDestino);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    return tarefaMapper.paraDTO(tarefaRepository.save(tarefa));
  }

  /**
   * Move a tarefa para outro projeto, herdando o workflow ativo do destino — confirmado na
   * entrevista de techspec.
   *
   * <p><b>TODO(TASK-04.1):</b> validar que o usuário é admin com permissão em ambos os projetos
   * (RBAC ainda não implementado nesta task).
   */
  @Transactional
  public TarefaDTO moverParaProjeto(UUID id, TarefaMoverProjetoRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    Projeto projetoDestino = buscarProjeto(request.projetoDestinoId());
    Workflow workflowDestino = buscarWorkflowAtivo(projetoDestino);
    Etapa etapaDestino = buscarEtapaDoWorkflow(request.etapaDestinoId(), workflowDestino);

    tarefa.setProjeto(projetoDestino);
    tarefa.setWorkflow(workflowDestino);
    tarefa.setEtapaAtual(etapaDestino);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    return tarefaMapper.paraDTO(tarefaRepository.save(tarefa));
  }

  /**
   * Marca/desmarca impedimento — independe da posição da tarefa no workflow (RF-004).
   *
   * <p>{@code request.motivo()} não é persistido aqui: o histórico de {@code Impedimento} (entidade
   * com início/fim, usada no cálculo de lead-time) é escopo da TASK-03.1.
   */
  @Transactional
  public TarefaDTO marcarImpedimento(UUID id, TarefaImpedimentoRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    tarefa.setImpedida(true);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    return tarefaMapper.paraDTO(tarefaRepository.save(tarefa));
  }

  @Transactional
  public TarefaDTO desmarcarImpedimento(UUID id) {
    Tarefa tarefa = buscarEntidade(id);
    tarefa.setImpedida(false);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    return tarefaMapper.paraDTO(tarefaRepository.save(tarefa));
  }

  private Projeto buscarProjeto(UUID projetoId) {
    return projetoRepository
        .findById(projetoId)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Projeto não encontrado: " + projetoId));
  }

  private Workflow buscarWorkflowAtivo(Projeto projeto) {
    if (projeto.getWorkflowAtivoId() == null) {
      throw new RegraDeNegocioException(
          "Projeto '%s' não possui workflow ativo definido.".formatted(projeto.getNome()));
    }
    return workflowRepository
        .findById(projeto.getWorkflowAtivoId())
        .orElseThrow(
            () ->
                new RecursoNaoEncontradoException(
                    "Workflow ativo não encontrado: " + projeto.getWorkflowAtivoId()));
  }

  private Etapa buscarEtapaDoWorkflow(UUID etapaId, Workflow workflow) {
    Etapa etapa =
        etapaRepository
            .findById(etapaId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Etapa não encontrada: " + etapaId));
    if (!etapa.getWorkflow().getId().equals(workflow.getId())) {
      throw new RegraDeNegocioException(
          "Etapa '%s' não pertence ao workflow '%s'."
              .formatted(etapa.getNome(), workflow.getNome()));
    }
    return etapa;
  }

  private Raia buscarRaiaOuNula(UUID raiaId) {
    if (raiaId == null) {
      return null;
    }
    return raiaRepository
        .findById(raiaId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Raia não encontrada: " + raiaId));
  }

  private Tarefa buscarEntidade(UUID id) {
    return tarefaRepository
        .findById(id)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Tarefa não encontrada: " + id));
  }
}
