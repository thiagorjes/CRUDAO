package com.crudao.kanban.domain.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.common.AcessoNegadoException;
import com.crudao.kanban.common.RecursoNaoEncontradoException;
import com.crudao.kanban.common.RegraDeNegocioException;
import com.crudao.kanban.domain.leadtime.RegistroEtapaService;
import com.crudao.kanban.domain.projeto.ConfiguracaoProjeto;
import com.crudao.kanban.domain.projeto.ConfiguracaoProjetoRepository;
import com.crudao.kanban.domain.projeto.Projeto;
import com.crudao.kanban.domain.projeto.ProjetoRepository;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.rbac.Usuario;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.TipoTransicao;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoEngine;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.realtime.EventoBoardPublisher;
import com.crudao.kanban.security.AutorizacaoProjetoService;
import com.crudao.kanban.security.UsuarioContexto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

  @Mock private TarefaRepository tarefaRepository;
  @Mock private ProjetoRepository projetoRepository;
  @Mock private WorkflowRepository workflowRepository;
  @Mock private EtapaRepository etapaRepository;
  @Mock private RaiaRepository raiaRepository;
  @Mock private ObservadorRepository observadorRepository;
  @Mock private TransicaoRepository transicaoRepository;
  @Mock private EventoBoardPublisher eventoBoardPublisher;
  @Mock private RegistroEtapaService registroEtapaService;
  @Mock private AutorizacaoProjetoService autorizacaoProjetoService;
  @Mock private UsuarioContexto usuarioContexto;
  @Mock private AuditoriaTarefaRepository auditoriaTarefaRepository;
  @Mock private ConfiguracaoProjetoRepository configuracaoProjetoRepository;

  private TarefaService tarefaService;
  private Workflow workflow;
  private Etapa etapaOrigem;
  private Etapa etapaDestino;
  private Etapa etapaSemTransicao;
  private Tarefa tarefa;
  private Usuario usuarioAtual;

  @BeforeEach
  void setUp() {
    tarefaService =
        new TarefaService(
            tarefaRepository,
            projetoRepository,
            workflowRepository,
            etapaRepository,
            raiaRepository,
            observadorRepository,
            transicaoRepository,
            new TransicaoEngine(),
            Mappers.getMapper(TarefaMapper.class),
            eventoBoardPublisher,
            registroEtapaService,
            autorizacaoProjetoService,
            usuarioContexto,
            auditoriaTarefaRepository,
            configuracaoProjetoRepository);

    workflow = new Workflow();
    workflow.setId(UUID.randomUUID());

    etapaOrigem = etapaComId(workflow);
    etapaDestino = etapaComId(workflow);
    etapaSemTransicao = etapaComId(workflow);
    etapaOrigem.setOrdem(0);
    etapaDestino.setOrdem(1);
    etapaSemTransicao.setOrdem(2);

    Projeto projeto = new Projeto();
    projeto.setId(UUID.randomUUID());

    tarefa = new Tarefa();
    tarefa.setId(UUID.randomUUID());
    tarefa.setProjeto(projeto);
    tarefa.setWorkflow(workflow);
    tarefa.setEtapaAtual(etapaOrigem);

    usuarioAtual = new Usuario();
    usuarioAtual.setId(UUID.randomUUID());

    lenient().when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
    lenient().when(usuarioContexto.usuarioAtual()).thenReturn(usuarioAtual);
    lenient()
        .when(etapaRepository.findByWorkflowIdOrderByOrdemAsc(workflow.getId()))
        .thenReturn(List.of(etapaOrigem, etapaDestino, etapaSemTransicao));
    lenient().when(autorizacaoProjetoService.temPermissao(any(), any(), any())).thenReturn(true);
  }

  private Etapa etapaComId(Workflow workflow) {
    Etapa etapa = new Etapa();
    etapa.setId(UUID.randomUUID());
    etapa.setWorkflow(workflow);
    etapa.setNome("etapa-" + UUID.randomUUID());
    return etapa;
  }

  @Test
  void deveMoverQuandoTransicaoPermitidaPeloWorkflow() {
    Transicao transicao = new Transicao();
    transicao.setEtapaOrigem(etapaOrigem);
    transicao.setEtapaDestino(etapaDestino);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(etapaRepository.findById(etapaDestino.getId())).thenReturn(Optional.of(etapaDestino));
    when(transicaoRepository.findByWorkflowId(workflow.getId())).thenReturn(List.of(transicao));

    TarefaDTO resultado =
        tarefaService.mover(tarefa.getId(), new TarefaMoverRequest(etapaDestino.getId()));

    assertThat(resultado.etapaAtualId()).isEqualTo(etapaDestino.getId());
  }

  @Test
  void deveMoverViaReaberturaQuandoTarefaEstaNaEtapaFinal() {
    etapaOrigem.setEtapaFinal(true);
    Transicao reabertura = new Transicao();
    reabertura.setEtapaOrigem(etapaOrigem);
    reabertura.setEtapaDestino(etapaDestino);
    reabertura.setTipo(TipoTransicao.REABERTURA);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(etapaRepository.findById(etapaDestino.getId())).thenReturn(Optional.of(etapaDestino));
    when(transicaoRepository.findByWorkflowId(workflow.getId())).thenReturn(List.of(reabertura));

    TarefaDTO resultado =
        tarefaService.mover(tarefa.getId(), new TarefaMoverRequest(etapaDestino.getId()));

    assertThat(resultado.etapaAtualId()).isEqualTo(etapaDestino.getId());
  }

  @Test
  void naoDeveMoverQuandoTransicaoNaoPermitidaPeloWorkflow() {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(etapaRepository.findById(etapaSemTransicao.getId()))
        .thenReturn(Optional.of(etapaSemTransicao));
    when(transicaoRepository.findByWorkflowId(workflow.getId())).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                tarefaService.mover(
                    tarefa.getId(), new TarefaMoverRequest(etapaSemTransicao.getId())))
        .isInstanceOf(RegraDeNegocioException.class);
  }

  @Test
  void deveMarcarImpedimentoIndependenteDaEtapaAtual() {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

    TarefaDTO resultado =
        tarefaService.marcarImpedimento(tarefa.getId(), new TarefaImpedimentoRequest(null));

    assertThat(resultado.impedida()).isTrue();
    assertThat(resultado.etapaAtualId()).isEqualTo(etapaOrigem.getId());
  }

  @Test
  void deveDesmarcarImpedimentoSemAlterarEtapaAtual() {
    tarefa.setImpedida(true);
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

    TarefaDTO resultado = tarefaService.desmarcarImpedimento(tarefa.getId());

    assertThat(resultado.impedida()).isFalse();
    assertThat(resultado.etapaAtualId()).isEqualTo(etapaOrigem.getId());
  }

  @Test
  void deveMoverParaOutroProjetoHerdandoWorkflowEEtapaDeDestino() {
    Projeto projetoDestino = new Projeto();
    projetoDestino.setId(UUID.randomUUID());
    projetoDestino.setNome("Projeto Destino");
    projetoDestino.setWorkflowAtivoId(UUID.randomUUID());

    Workflow workflowDestino = new Workflow();
    workflowDestino.setId(projetoDestino.getWorkflowAtivoId());

    Etapa etapaDestinoOutroProjeto = etapaComId(workflowDestino);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(projetoRepository.findById(projetoDestino.getId()))
        .thenReturn(Optional.of(projetoDestino));
    when(workflowRepository.findById(workflowDestino.getId()))
        .thenReturn(Optional.of(workflowDestino));
    when(etapaRepository.findById(etapaDestinoOutroProjeto.getId()))
        .thenReturn(Optional.of(etapaDestinoOutroProjeto));

    TarefaDTO resultado =
        tarefaService.moverParaProjeto(
            tarefa.getId(),
            new TarefaMoverProjetoRequest(
                projetoDestino.getId(), etapaDestinoOutroProjeto.getId()));

    assertThat(resultado.projetoId()).isEqualTo(projetoDestino.getId());
    assertThat(resultado.workflowId()).isEqualTo(workflowDestino.getId());
    assertThat(resultado.etapaAtualId()).isEqualTo(etapaDestinoOutroProjeto.getId());
  }

  @Test
  void naoDeveMoverParaProjetoSemWorkflowAtivoDefinido() {
    Projeto projetoDestino = new Projeto();
    projetoDestino.setId(UUID.randomUUID());
    projetoDestino.setNome("Projeto Sem Workflow");

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(projetoRepository.findById(projetoDestino.getId()))
        .thenReturn(Optional.of(projetoDestino));

    assertThatThrownBy(
            () ->
                tarefaService.moverParaProjeto(
                    tarefa.getId(),
                    new TarefaMoverProjetoRequest(projetoDestino.getId(), UUID.randomUUID())))
        .isInstanceOf(RegraDeNegocioException.class);
  }

  @Test
  void naoDeveMoverParaProjetoDestinoInexistente() {
    UUID projetoInexistenteId = UUID.randomUUID();
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(projetoRepository.findById(projetoInexistenteId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                tarefaService.moverParaProjeto(
                    tarefa.getId(),
                    new TarefaMoverProjetoRequest(projetoInexistenteId, UUID.randomUUID())))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveMarcarIniciadaAoSairDaEtapaInicialEGravarAuditoriaDeEtapa() {
    Transicao transicao = new Transicao();
    transicao.setEtapaOrigem(etapaOrigem);
    transicao.setEtapaDestino(etapaDestino);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(etapaRepository.findById(etapaDestino.getId())).thenReturn(Optional.of(etapaDestino));
    when(transicaoRepository.findByWorkflowId(workflow.getId())).thenReturn(List.of(transicao));

    tarefaService.mover(tarefa.getId(), new TarefaMoverRequest(etapaDestino.getId()));

    assertThat(tarefa.isIniciada()).isTrue();
    verify(auditoriaTarefaRepository).save(any(AuditoriaTarefa.class));
  }

  @Test
  void deveExigirPermissaoFinalizarAoMoverParaEtapaFinal() {
    etapaDestino.setEtapaFinal(true);
    Transicao transicao = new Transicao();
    transicao.setEtapaOrigem(etapaOrigem);
    transicao.setEtapaDestino(etapaDestino);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(etapaRepository.findById(etapaDestino.getId())).thenReturn(Optional.of(etapaDestino));
    when(transicaoRepository.findByWorkflowId(workflow.getId())).thenReturn(List.of(transicao));

    tarefaService.mover(tarefa.getId(), new TarefaMoverRequest(etapaDestino.getId()));

    verify(autorizacaoProjetoService)
        .exigirPermissao(usuarioAtual, tarefa.getProjeto().getId(), "tarefa:finalizar");
  }

  @Test
  void devSemToggleNaoEditaTituloDeTarefaIniciada() {
    tarefa.setIniciada(true);
    tarefa.setTitulo("Original");
    ConfiguracaoProjeto configuracao = new ConfiguracaoProjeto();
    configuracao.setDevPodeEditarTarefaIniciada(false);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.temPermissao(
            usuarioAtual, tarefa.getProjeto().getId(), "tarefa:atribuir"))
        .thenReturn(false);
    when(configuracaoProjetoRepository.findById(tarefa.getProjeto().getId()))
        .thenReturn(Optional.of(configuracao));

    assertThatThrownBy(
            () ->
                tarefaService.editar(
                    tarefa.getId(),
                    new TarefaRequest(
                        tarefa.getProjeto().getId(),
                        null,
                        null,
                        TipoTarefa.FEATURE,
                        "Novo",
                        null,
                        null)))
        .isInstanceOf(AcessoNegadoException.class);
  }

  @Test
  void devComToggleEditaTituloDeTarefaIniciada() {
    tarefa.setIniciada(true);
    tarefa.setTitulo("Original");
    ConfiguracaoProjeto configuracao = new ConfiguracaoProjeto();
    configuracao.setDevPodeEditarTarefaIniciada(true);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.temPermissao(
            usuarioAtual, tarefa.getProjeto().getId(), "tarefa:atribuir"))
        .thenReturn(false);
    when(configuracaoProjetoRepository.findById(tarefa.getProjeto().getId()))
        .thenReturn(Optional.of(configuracao));

    TarefaDTO resultado =
        tarefaService.editar(
            tarefa.getId(),
            new TarefaRequest(
                tarefa.getProjeto().getId(), null, null, TipoTarefa.FEATURE, "Novo", null, null));

    assertThat(resultado.titulo()).isEqualTo("Novo");
    verify(auditoriaTarefaRepository).save(any(AuditoriaTarefa.class));
  }

  @Test
  void productOwnerEditaTituloDeTarefaIniciadaSemToggle() {
    tarefa.setIniciada(true);
    tarefa.setTitulo("Original");

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.temPermissao(
            usuarioAtual, tarefa.getProjeto().getId(), "tarefa:atribuir"))
        .thenReturn(true);

    TarefaDTO resultado =
        tarefaService.editar(
            tarefa.getId(),
            new TarefaRequest(
                tarefa.getProjeto().getId(), null, null, TipoTarefa.FEATURE, "Novo", null, null));

    assertThat(resultado.titulo()).isEqualTo("Novo");
  }

  @Test
  void devSemToggleNaoExcluiTarefa() {
    ConfiguracaoProjeto configuracao = new ConfiguracaoProjeto();
    configuracao.setDevPodeExcluirTarefa(false);

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.temPermissao(
            usuarioAtual, tarefa.getProjeto().getId(), "tarefa:atribuir"))
        .thenReturn(false);
    when(configuracaoProjetoRepository.findById(tarefa.getProjeto().getId()))
        .thenReturn(Optional.of(configuracao));

    assertThatThrownBy(() -> tarefaService.excluir(tarefa.getId()))
        .isInstanceOf(AcessoNegadoException.class);
  }

  @Test
  void devPuxaTarefaJaAtribuidaAOutroSemAprovacao() {
    tarefa.setResponsavelId(UUID.randomUUID());

    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.usuarioTemAcessoAoProjeto(
            usuarioAtual, tarefa.getProjeto().getId()))
        .thenReturn(true);

    TarefaDTO resultado =
        tarefaService.atribuir(
            tarefa.getId(), new AtribuirResponsavelRequest(usuarioAtual.getId()));

    assertThat(resultado.responsavelId()).isEqualTo(usuarioAtual.getId());
    verify(auditoriaTarefaRepository).save(any(AuditoriaTarefa.class));
  }

  @Test
  void naoPuxaTarefaDeProjetoFinalizado() {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    org.mockito.Mockito.doThrow(new RegraDeNegocioException("Projeto finalizado"))
        .when(autorizacaoProjetoService)
        .exigirProjetoNaoFinalizado(tarefa.getProjeto().getId());

    assertThatThrownBy(
            () ->
                tarefaService.atribuir(
                    tarefa.getId(), new AtribuirResponsavelRequest(usuarioAtual.getId())))
        .isInstanceOf(RegraDeNegocioException.class);
    verify(auditoriaTarefaRepository, never()).save(any());
  }

  @Test
  void devNaoAtribuiTarefaATerceiroSemPermissao() {
    UUID terceiro = UUID.randomUUID();
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    org.mockito.Mockito.doThrow(new AcessoNegadoException("sem permissão"))
        .when(autorizacaoProjetoService)
        .exigirPermissao(usuarioAtual, tarefa.getProjeto().getId(), "tarefa:atribuir");

    assertThatThrownBy(
            () -> tarefaService.atribuir(tarefa.getId(), new AtribuirResponsavelRequest(terceiro)))
        .isInstanceOf(AcessoNegadoException.class);
    verify(auditoriaTarefaRepository, never()).save(any());
  }

  @Test
  void historicoRetornaListaQuandoUsuarioTemAcessoAoProjeto() {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.usuarioTemAcessoAoProjeto(
            usuarioAtual, tarefa.getProjeto().getId()))
        .thenReturn(true);
    when(auditoriaTarefaRepository.historicoPorTarefa(tarefa.getId())).thenReturn(List.of());

    assertThat(tarefaService.historico(tarefa.getId())).isEmpty();
  }

  @Test
  void historicoNegaAcessoSemVinculoAoProjeto() {
    when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
    when(autorizacaoProjetoService.usuarioTemAcessoAoProjeto(
            usuarioAtual, tarefa.getProjeto().getId()))
        .thenReturn(false);

    assertThatThrownBy(() -> tarefaService.historico(tarefa.getId()))
        .isInstanceOf(AcessoNegadoException.class);
  }
}
