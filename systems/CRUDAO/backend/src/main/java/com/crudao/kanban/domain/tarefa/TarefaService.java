package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.domain.leadtime.RegistroEtapaService;
import com.crudao.kanban.domain.projeto.ConfiguracaoProjeto;
import com.crudao.kanban.domain.projeto.ConfiguracaoProjetoRepository;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoEngine;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.realtime.EventoBoardPublisher;
import com.crudao.kanban.realtime.TipoEventoBoard;
import com.crudao.kanban.security.AutorizacaoProjetoService;
import com.crudao.kanban.security.UsuarioContexto;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** CRUD e movimentação de Tarefa — RF-002, RF-003, RF-004, RF-012. */
@Service
@RequiredArgsConstructor
public class TarefaService {

  private static final String PERMISSAO_TAREFA = "tarefa:gerenciar";
  private static final String PERMISSAO_IMPEDIMENTO = "impedimento:marcar";
  private static final String PERMISSAO_ATRIBUIR = "tarefa:atribuir";
  private static final String PERMISSAO_FINALIZAR = "tarefa:finalizar";

  private final TarefaRepository tarefaRepository;
  private final ProjetoRepository projetoRepository;
  private final WorkflowRepository workflowRepository;
  private final EtapaRepository etapaRepository;
  private final RaiaRepository raiaRepository;
  private final ObservadorRepository observadorRepository;
  private final TransicaoRepository transicaoRepository;
  private final TransicaoEngine transicaoEngine;
  private final TarefaMapper tarefaMapper;
  private final EventoBoardPublisher eventoBoardPublisher;
  private final RegistroEtapaService registroEtapaService;
  private final AutorizacaoProjetoService autorizacaoProjetoService;
  private final UsuarioContexto usuarioContexto;
  private final AuditoriaTarefaRepository auditoriaTarefaRepository;
  private final ConfiguracaoProjetoRepository configuracaoProjetoRepository;

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
    exigirPermissao(request.projetoId(), PERMISSAO_TAREFA);
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
    Tarefa salva = tarefaRepository.save(tarefa);
    registroEtapaService.abrirRegistro(salva, etapaInicial);
    publicarAposCommit(TipoEventoBoard.TAREFA_CRIADA, salva);
    return tarefaMapper.paraDTO(salva);
  }

  @Transactional
  public TarefaDTO editar(UUID id, TarefaRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    UUID projetoId = tarefa.getProjeto().getId();
    exigirPermissao(projetoId, PERMISSAO_TAREFA);
    exigirEdicaoLiberada(tarefa, projetoId);

    String tituloAnterior = tarefa.getTitulo();
    String descricaoAnterior = tarefa.getDescricao();

    tarefa.setRaia(buscarRaiaOuNula(request.raiaId()));
    tarefa.setTipo(request.tipo());
    tarefa.setTitulo(request.titulo());
    tarefa.setDescricao(request.descricao());
    tarefa.setResponsavelId(request.responsavelId());
    tarefa.setAtualizadoEm(java.time.Instant.now());
    Tarefa salva = tarefaRepository.save(tarefa);

    registrarAuditoria(salva, CampoAuditoria.TITULO, tituloAnterior, request.titulo());
    registrarAuditoria(salva, CampoAuditoria.DESCRICAO, descricaoAnterior, request.descricao());
    return tarefaMapper.paraDTO(salva);
  }

  @Transactional
  public void excluir(UUID id) {
    Tarefa tarefa = buscarEntidade(id);
    UUID projetoId = tarefa.getProjeto().getId();
    exigirPermissao(projetoId, PERMISSAO_TAREFA);
    if (ehDevTier(projetoId) && !buscarConfiguracao(projetoId).isDevPodeExcluirTarefa()) {
      throw new AcessoNegadoException(
          "Dev sem permissão para excluir tarefas neste projeto (toggle desabilitado).");
    }
    observadorRepository.deleteAll(observadorRepository.findByTarefaId(id));
    tarefaRepository.delete(tarefa);
  }

  /**
   * Atribui/"puxa" a responsabilidade pela tarefa — RN-012. "Puxar" ({@code usuarioId} igual ao
   * autenticado) é livre para qualquer membro do projeto, mesmo já atribuída a outro; atribuir a um
   * terceiro exige {@code tarefa:atribuir}.
   */
  @Transactional
  public TarefaDTO atribuir(UUID id, AtribuirResponsavelRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    UUID projetoId = tarefa.getProjeto().getId();
    Usuario usuario = usuarioContexto.usuarioAtual();
    autorizacaoProjetoService.exigirProjetoNaoFinalizado(projetoId);

    if (request.usuarioId().equals(usuario.getId())) {
      if (!autorizacaoProjetoService.usuarioTemAcessoAoProjeto(usuario, projetoId)) {
        throw new AcessoNegadoException("Usuário não é membro do projeto '" + projetoId + "'.");
      }
    } else {
      exigirPermissao(projetoId, PERMISSAO_ATRIBUIR);
    }

    UUID responsavelAnterior = tarefa.getResponsavelId();
    tarefa.setResponsavelId(request.usuarioId());
    tarefa.setAtualizadoEm(java.time.Instant.now());
    Tarefa salva = tarefaRepository.save(tarefa);
    registrarAuditoria(
        salva,
        CampoAuditoria.RESPONSAVEL,
        responsavelAnterior != null ? responsavelAnterior.toString() : null,
        request.usuarioId().toString());
    return tarefaMapper.paraDTO(salva);
  }

  /** RF-017: histórico de auditoria da tarefa, ordenado do mais recente para o mais antigo. */
  @Transactional(readOnly = true)
  public List<AuditoriaTarefaDTO> historico(UUID id) {
    Tarefa tarefa = buscarEntidade(id);
    UUID projetoId = tarefa.getProjeto().getId();
    Usuario usuario = usuarioContexto.usuarioAtual();
    if (!autorizacaoProjetoService.usuarioTemAcessoAoProjeto(usuario, projetoId)) {
      throw new AcessoNegadoException("Usuário sem acesso ao projeto da tarefa.");
    }
    return auditoriaTarefaRepository.historicoPorTarefa(id);
  }

  /**
   * Move a tarefa para outra etapa do mesmo workflow — RF-002. Só é permitido se houver {@link
   * Transicao} (NORMAL ou REABERTURA, RF-012) ligando a etapa atual à etapa destino. O estado de
   * impedimento não interfere nesta regra (RF-004).
   */
  @Transactional
  public TarefaDTO mover(UUID id, TarefaMoverRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    UUID projetoId = tarefa.getProjeto().getId();
    exigirPermissao(projetoId, PERMISSAO_TAREFA);
    Etapa etapaOrigem = tarefa.getEtapaAtual();
    Etapa etapaDestino = buscarEtapaDoWorkflow(request.etapaDestinoId(), tarefa.getWorkflow());

    List<Transicao> transicoes = transicaoRepository.findByWorkflowId(tarefa.getWorkflow().getId());
    if (!transicaoEngine.transicaoPermitida(etapaOrigem, etapaDestino, transicoes)) {
      throw new RegraDeNegocioException(
          "Transição não permitida pelo workflow: '%s' -> '%s'"
              .formatted(etapaOrigem.getNome(), etapaDestino.getNome()));
    }
    exigirPermissaoFinalizarSeNecessario(projetoId, etapaOrigem, etapaDestino);
    marcarIniciadaSeSaiuDaInicial(tarefa, etapaOrigem);

    registroEtapaService.fecharRegistroAtual(tarefa);
    tarefa.setEtapaAtual(etapaDestino);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    Tarefa salva = tarefaRepository.save(tarefa);
    registroEtapaService.abrirRegistro(salva, etapaDestino);
    registrarAuditoria(salva, CampoAuditoria.ETAPA, etapaOrigem.getNome(), etapaDestino.getNome());
    publicarAposCommit(TipoEventoBoard.TAREFA_MOVIDA, salva);
    return tarefaMapper.paraDTO(salva);
  }

  /**
   * Move a tarefa para outro projeto, herdando o workflow ativo do destino — confirmado na
   * entrevista de techspec. Exige {@code tarefa:gerenciar} nos DOIS projetos (origem e destino) —
   * achado do /analyze, finding G2: contrato explicitado na TechSpec v1.3.
   */
  @Transactional
  public TarefaDTO moverParaProjeto(UUID id, TarefaMoverProjetoRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    UUID projetoOrigemId = tarefa.getProjeto().getId();
    exigirPermissao(projetoOrigemId, PERMISSAO_TAREFA);
    exigirPermissao(request.projetoDestinoId(), PERMISSAO_TAREFA);
    Projeto projetoDestino = buscarProjeto(request.projetoDestinoId());
    Workflow workflowDestino = buscarWorkflowAtivo(projetoDestino);
    Etapa etapaOrigem = tarefa.getEtapaAtual();
    Etapa etapaDestino = buscarEtapaDoWorkflow(request.etapaDestinoId(), workflowDestino);

    if (etapaOrigem.isEtapaFinal()) {
      exigirPermissao(projetoOrigemId, PERMISSAO_FINALIZAR);
    }
    if (etapaDestino.isEtapaFinal()) {
      exigirPermissao(request.projetoDestinoId(), PERMISSAO_FINALIZAR);
    }
    marcarIniciadaSeSaiuDaInicial(tarefa, etapaOrigem);

    registroEtapaService.fecharRegistroAtual(tarefa);
    tarefa.setProjeto(projetoDestino);
    tarefa.setWorkflow(workflowDestino);
    tarefa.setEtapaAtual(etapaDestino);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    Tarefa salva = tarefaRepository.save(tarefa);
    registroEtapaService.abrirRegistro(salva, etapaDestino);
    registrarAuditoria(salva, CampoAuditoria.ETAPA, etapaOrigem.getNome(), etapaDestino.getNome());
    publicarAposCommit(TipoEventoBoard.TAREFA_MOVIDA, salva);
    return tarefaMapper.paraDTO(salva);
  }

  /**
   * Marca/desmarca impedimento — independe da posição da tarefa no workflow (RF-004). Abre/fecha um
   * {@link com.crudao.kanban.domain.leadtime.Impedimento} vinculado ao registro de permanência
   * atual da tarefa, usado no cálculo de lead-time de impedimento (RN-002).
   */
  @Transactional
  public TarefaDTO marcarImpedimento(UUID id, TarefaImpedimentoRequest request) {
    Tarefa tarefa = buscarEntidade(id);
    exigirPermissao(tarefa.getProjeto().getId(), PERMISSAO_IMPEDIMENTO);
    tarefa.setImpedida(true);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    Tarefa salva = tarefaRepository.save(tarefa);
    registroEtapaService.abrirImpedimento(salva, request.motivo());
    publicarAposCommit(TipoEventoBoard.IMPEDIMENTO_ALTERADO, salva);
    return tarefaMapper.paraDTO(salva);
  }

  @Transactional
  public TarefaDTO desmarcarImpedimento(UUID id) {
    Tarefa tarefa = buscarEntidade(id);
    exigirPermissao(tarefa.getProjeto().getId(), PERMISSAO_IMPEDIMENTO);
    tarefa.setImpedida(false);
    tarefa.setAtualizadoEm(java.time.Instant.now());
    Tarefa salva = tarefaRepository.save(tarefa);
    registroEtapaService.fecharImpedimentoAtual(salva);
    publicarAposCommit(TipoEventoBoard.IMPEDIMENTO_ALTERADO, salva);
    return tarefaMapper.paraDTO(salva);
  }

  /** Observadores da tarefa (RN-007) — usuários notificados a cada transição de etapa (RF-005). */
  @Transactional(readOnly = true)
  public List<UUID> listarObservadores(UUID tarefaId) {
    return observadorRepository.findByTarefaId(tarefaId).stream()
        .map(Observador::getUsuarioId)
        .toList();
  }

  @Transactional
  public void adicionarObservador(UUID tarefaId, UUID usuarioId) {
    Tarefa tarefa = buscarEntidade(tarefaId);
    if (observadorRepository.existsByTarefaIdAndUsuarioId(tarefaId, usuarioId)) {
      return;
    }
    Observador observador = new Observador();
    observador.setTarefa(tarefa);
    observador.setUsuarioId(usuarioId);
    observadorRepository.save(observador);
  }

  @Transactional
  public void removerObservador(UUID tarefaId, UUID usuarioId) {
    observadorRepository.deleteByTarefaIdAndUsuarioId(tarefaId, usuarioId);
  }

  /**
   * Publica o evento de board somente após o commit da transação (ADR-004): o pod receptor busca o
   * estado da tarefa no banco ao receber a notificação, então a linha precisa já estar persistida.
   */
  private void publicarAposCommit(TipoEventoBoard tipo, Tarefa tarefa) {
    UUID tarefaId = tarefa.getId();
    UUID projetoId = tarefa.getProjeto().getId();
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      eventoBoardPublisher.publicar(tipo, tarefaId, projetoId);
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            eventoBoardPublisher.publicar(tipo, tarefaId, projetoId);
          }
        });
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

  private void exigirPermissao(UUID projetoId, String permissao) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    autorizacaoProjetoService.exigirPermissao(usuario, projetoId, permissao);
  }

  /** RN-011: transição de/para etapa final exige {@code tarefa:finalizar}, na ida e na volta. */
  private void exigirPermissaoFinalizarSeNecessario(
      UUID projetoId, Etapa etapaOrigem, Etapa etapaDestino) {
    if (etapaDestino.isEtapaFinal() || etapaOrigem.isEtapaFinal()) {
      exigirPermissao(projetoId, PERMISSAO_FINALIZAR);
    }
  }

  /**
   * RN-009/RN-010: marca {@code iniciada=true} na primeira vez que a tarefa sai da etapa inicial do
   * workflow (menor {@code ordem}) — idempotente, nunca desmarca.
   */
  private void marcarIniciadaSeSaiuDaInicial(Tarefa tarefa, Etapa etapaOrigem) {
    if (tarefa.isIniciada()) {
      return;
    }
    Etapa etapaInicial = buscarEtapaInicial(tarefa.getWorkflow());
    if (etapaInicial != null && etapaOrigem.getId().equals(etapaInicial.getId())) {
      tarefa.setIniciada(true);
    }
  }

  private Etapa buscarEtapaInicial(Workflow workflow) {
    List<Etapa> etapas = etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId());
    return etapas.isEmpty() ? null : etapas.get(0);
  }

  /**
   * RN-009/RN-010: {@code dev} (papel com {@code tarefa:gerenciar} mas sem {@code tarefa:atribuir})
   * só edita título/descrição/tipo de tarefa já iniciada se o toggle {@code
   * devPodeEditarTarefaIniciada} estiver ligado. Demais papéis com {@code tarefa:gerenciar} não têm
   * essa restrição.
   */
  private void exigirEdicaoLiberada(Tarefa tarefa, UUID projetoId) {
    if (!tarefa.isIniciada() || !ehDevTier(projetoId)) {
      return;
    }
    if (!buscarConfiguracao(projetoId).isDevPodeEditarTarefaIniciada()) {
      throw new AcessoNegadoException(
          "Tarefa já iniciada: dev não pode editar título/descrição/tipo sem o toggle habilitado.");
    }
  }

  /**
   * Diferencia {@code dev} dos demais papéis com {@code tarefa:gerenciar} (product_owner,
   * project_admin, admin) — todos eles também têm {@code tarefa:atribuir}, que {@code dev} nunca
   * tem (RN-011/RN-012).
   */
  private boolean ehDevTier(UUID projetoId) {
    Usuario usuario = usuarioContexto.usuarioAtual();
    return !autorizacaoProjetoService.temPermissao(usuario, projetoId, PERMISSAO_ATRIBUIR);
  }

  private ConfiguracaoProjeto buscarConfiguracao(UUID projetoId) {
    return configuracaoProjetoRepository
        .findById(projetoId)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Configuração não encontrada: " + projetoId));
  }

  /** RF-017: grava a linha de auditoria só quando o valor de fato mudou. */
  private void registrarAuditoria(
      Tarefa tarefa, CampoAuditoria campo, String valorAnterior, String valorNovo) {
    if (Objects.equals(valorAnterior, valorNovo)) {
      return;
    }
    Usuario usuario = usuarioContexto.usuarioAtual();
    AuditoriaTarefa registro = new AuditoriaTarefa();
    registro.setTarefa(tarefa);
    registro.setUsuarioId(usuario.getId());
    registro.setCampo(campo);
    registro.setValorAnterior(valorAnterior);
    registro.setValorNovo(valorNovo);
    auditoriaTarefaRepository.save(registro);
  }
}
