package com.crudao.kanban.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaAuditoria;
import com.crudao.kanban.domain.tarefa.TarefaAuditoriaRepository;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico;
import com.crudao.kanban.domain.tarefa.TarefaEtapaHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistorico;
import com.crudao.kanban.domain.tarefa.TarefaImpedimentoHistoricoRepository;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.EtapaRepository;
import com.crudao.kanban.domain.workflow.Transicao;
import com.crudao.kanban.domain.workflow.TransicaoRepository;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.evento.EventoBoardPublisher;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Criação de card pelo board — TASK-04.1 (RF-018, RN-CB-001 a RN-CB-005). Mover/editar/detalhe —
 * TASK-04.2 (RF-002, RF-003, RF-006, RF-012, RN-011, RN-012, RN-016).
 */
@ExtendWith(MockitoExtension.class)
class TarefaServiceTest {

    private static final String PERMISSAO_GERENCIAR = "tarefa:gerenciar";
    private static final String PERMISSAO_FINALIZAR = "tarefa:finalizar";
    private static final String PERMISSAO_IMPEDIMENTO = "tarefa:impedimento";

    @Mock private TarefaRepository tarefaRepository;
    @Mock private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;
    @Mock private TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;
    @Mock private TarefaAuditoriaRepository tarefaAuditoriaRepository;
    @Mock private TarefaObservadorRepository tarefaObservadorRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private EtapaRepository etapaRepository;
    @Mock private TransicaoRepository transicaoRepository;
    @Mock private RaiaRepository raiaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    @Mock private PermissaoGuard permissaoGuard;
    @Mock private EventoBoardPublisher eventoBoardPublisher;
    @Mock private com.crudao.kanban.notificacao.NotificacaoService notificacaoService;
    @Mock private com.crudao.kanban.domain.notificacao.NotificacaoRepository notificacaoRepository;

    private TarefaService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID workflowId = UUID.randomUUID();
    private final Usuario usuarioAutenticado = usuarioAutenticado();

    @BeforeEach
    void setUp() {
        service =
                new TarefaService(
                        tarefaRepository,
                        tarefaEtapaHistoricoRepository,
                        tarefaImpedimentoHistoricoRepository,
                        tarefaAuditoriaRepository,
                        tarefaObservadorRepository,
                        notificacaoRepository,
                        projetoRepository,
                        workflowRepository,
                        etapaRepository,
                        transicaoRepository,
                        raiaRepository,
                        usuarioRepository,
                        usuarioProjetoPapelRepository,
                        permissaoGuard,
                        eventoBoardPublisher,
                        notificacaoService);
        UsuarioAutenticadoHolder.set(usuarioAutenticado);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void criar_semPermissao_lanca403() {
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "tarefa:gerenciar");

        assertThatThrownBy(() -> service.criar(projetoId, request(null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_projetoFinalizado_lanca403() {
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.criar(projetoId, request(null, null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_semTitulo_lanca422() {
        assertThatThrownBy(() -> service.criar(projetoId, new CriarTarefaRequest("  ", null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_semWorkflowConfigurado_lanca422() {
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.criar(projetoId, request(null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
    }

    @Test
    void criar_workflowSemEtapas_lanca422() {
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow()));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.criar(projetoId, request(null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_responsavelInexistente_lanca422() {
        Workflow workflow = workflow();
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of(etapa(0)));
        when(raiaRepository.findByProjetoIdOrderByOrdem(projetoId)).thenReturn(List.of(raia(projeto())));
        UUID responsavelId = UUID.randomUUID();
        when(usuarioRepository.findById(responsavelId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(projetoId, request(responsavelId, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void criar_semResponsavelSemRaia_usaDefaults() {
        Workflow workflow = workflow();
        Etapa e0 = etapa(0);
        Etapa e1 = etapa(1);
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of(e0, e1));
        when(raiaRepository.findByProjetoIdOrderByOrdem(projetoId)).thenReturn(List.of());
        Raia raiaGlobal = raia(null);
        when(raiaRepository.findByProjetoIdIsNull()).thenReturn(List.of(raiaGlobal));
        when(projetoRepository.getReferenceById(projetoId)).thenReturn(projeto());
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponse response = service.criar(projetoId, request(null, null));

        assertThat(response.etapaAtualId()).isEqualTo(e0.getId());
        assertThat(response.raiaId()).isEqualTo(raiaGlobal.getId());
        assertThat(response.responsavelId()).isNull();

        var historicoCaptor = org.mockito.ArgumentCaptor.forClass(
                com.crudao.kanban.domain.tarefa.TarefaEtapaHistorico.class);
        verify(tarefaEtapaHistoricoRepository).save(historicoCaptor.capture());
        var historico = historicoCaptor.getValue();
        assertThat(historico.getEtapa()).isEqualTo(e0);
        assertThat(historico.getEntradaEm()).isNotNull();
        assertThat(historico.getSaidaEm()).isNull();

        verify(eventoBoardPublisher)
                .publicar(projetoId, com.crudao.kanban.evento.TipoEventoBoard.TAREFA_CRIADA, response.id());
    }

    @Test
    void criar_comRaiaDoProprioProjeto_usaRaiaInformada() {
        Workflow workflow = workflow();
        Etapa e0 = etapa(0);
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of(e0));
        Raia raiaDoProjeto = raia(projeto());
        when(raiaRepository.findById(raiaDoProjeto.getId())).thenReturn(Optional.of(raiaDoProjeto));
        when(projetoRepository.getReferenceById(projetoId)).thenReturn(projeto());
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TarefaResponse response = service.criar(projetoId, request(null, raiaDoProjeto.getId()));

        assertThat(response.raiaId()).isEqualTo(raiaDoProjeto.getId());
        verify(raiaRepository, never()).findByProjetoIdOrderByOrdem(any());
    }

    @Test
    void criar_comRaiaDeOutroProjeto_lanca422() {
        Workflow workflow = workflow();
        when(workflowRepository.findByProjetoId(projetoId)).thenReturn(List.of(workflow));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of(etapa(0)));
        Projeto outroProjeto = new Projeto();
        outroProjeto.setId(UUID.randomUUID());
        Raia raiaDeOutroProjeto = raia(outroProjeto);
        when(raiaRepository.findById(raiaDeOutroProjeto.getId())).thenReturn(Optional.of(raiaDeOutroProjeto));

        assertThatThrownBy(
                        () -> service.criar(projetoId, request(null, raiaDeOutroProjeto.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(tarefaRepository, never()).save(any());
    }

    private CriarTarefaRequest request(UUID responsavelId, UUID raiaId) {
        return new CriarTarefaRequest("Nova tarefa", null, responsavelId, raiaId);
    }

    // ---- TASK-04.2: mover ----

    @Test
    void mover_transicaoNaoConfigurada_lanca409() {
        Workflow workflow = workflow();
        Etapa origem = etapaComWorkflow(0, workflow, false);
        Etapa destino = etapaComWorkflow(1, workflow, false);
        Tarefa tarefa = tarefa(origem, workflow, false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(etapaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        when(transicaoRepository.findByEtapaOrigemId(origem.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.mover(tarefa.getId(), new MoverTarefaRequest(destino.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void mover_paraEtapaFinalSemPermissaoFinalizar_lanca403() {
        Workflow workflow = workflow();
        Etapa origem = etapaComWorkflow(0, workflow, false);
        Etapa destino = etapaComWorkflow(1, workflow, true);
        Tarefa tarefa = tarefa(origem, workflow, false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(etapaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        when(transicaoRepository.findByEtapaOrigemId(origem.getId()))
                .thenReturn(List.of(transicao(origem, destino)));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, PERMISSAO_FINALIZAR);

        assertThatThrownBy(() -> service.mover(tarefa.getId(), new MoverTarefaRequest(destino.getId())))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void mover_desfinalizarSemPermissaoFinalizar_lanca403() {
        Workflow workflow = workflow();
        Etapa origem = etapaComWorkflow(1, workflow, true);
        Etapa destino = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(origem, workflow, true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(etapaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        when(transicaoRepository.findByEtapaOrigemId(origem.getId()))
                .thenReturn(List.of(transicao(origem, destino)));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, PERMISSAO_FINALIZAR);

        assertThatThrownBy(() -> service.mover(tarefa.getId(), new MoverTarefaRequest(destino.getId())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void mover_transicaoValida_fechaHistoricoAtualAbreNovoEIniciaTarefa() {
        Workflow workflow = workflow();
        Etapa origem = etapaComWorkflow(0, workflow, false);
        Etapa destino = etapaComWorkflow(1, workflow, false);
        Tarefa tarefa = tarefa(origem, workflow, false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(etapaRepository.findById(destino.getId())).thenReturn(Optional.of(destino));
        when(transicaoRepository.findByEtapaOrigemId(origem.getId()))
                .thenReturn(List.of(transicao(origem, destino)));
        when(etapaRepository.findByWorkflowIdOrderByOrdem(workflowId)).thenReturn(List.of(origem, destino));
        TarefaEtapaHistorico historicoAberto = new TarefaEtapaHistorico();
        historicoAberto.setTarefa(tarefa);
        historicoAberto.setEtapa(origem);
        historicoAberto.setEntradaEm(OffsetDateTime.now().minusHours(1));
        when(tarefaEtapaHistoricoRepository.findByTarefaIdAndSaidaEmIsNull(tarefa.getId()))
                .thenReturn(Optional.of(historicoAberto));
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.mover(tarefa.getId(), new MoverTarefaRequest(destino.getId()));

        assertThat(historicoAberto.getSaidaEm()).isNotNull();
        var novoHistoricoCaptor = ArgumentCaptor.forClass(TarefaEtapaHistorico.class);
        verify(tarefaEtapaHistoricoRepository, org.mockito.Mockito.times(2))
                .save(novoHistoricoCaptor.capture());
        TarefaEtapaHistorico novo = novoHistoricoCaptor.getAllValues().get(1);
        assertThat(novo.getEtapa()).isEqualTo(destino);
        assertThat(novo.getSaidaEm()).isNull();
        assertThat(tarefa.isIniciada()).isTrue();
        assertThat(tarefa.getEtapaAtual()).isEqualTo(destino);

        var auditoriaCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditoriaCaptor.capture());
        assertThat(auditoriaCaptor.getValue().getCampo()).isEqualTo("etapa");

        verify(eventoBoardPublisher)
                .publicar(projetoId, com.crudao.kanban.evento.TipoEventoBoard.TAREFA_MOVIDA, tarefa.getId());
        verify(notificacaoService)
                .notificarObservadores(
                        org.mockito.ArgumentMatchers.eq(tarefa),
                        org.mockito.ArgumentMatchers.eq(
                                com.crudao.kanban.notificacao.NotificacaoService.TIPO_TRANSICAO_ETAPA),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void mover_tarefaInexistente_lanca404() {
        UUID id = UUID.randomUUID();
        when(tarefaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.mover(id, new MoverTarefaRequest(UUID.randomUUID())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void mover_projetoFinalizado_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.mover(tarefa.getId(), new MoverTarefaRequest(UUID.randomUUID())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    // ---- TASK-04.2: editar ----

    @Test
    void editar_removerResponsavel_desatribuiComGerenciarEGeraAuditoria() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, false);
        Usuario responsavelAtual = new Usuario();
        responsavelAtual.setId(UUID.randomUUID());
        tarefa.setResponsavel(responsavelAtual);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(tarefa.getId(), new EditarTarefaRequest(null, null, null, true));

        assertThat(tarefa.getResponsavel()).isNull();
        verify(permissaoGuard).exigir(projetoId, PERMISSAO_GERENCIAR);
        var auditoriaCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditoriaCaptor.capture());
        assertThat(auditoriaCaptor.getValue().getValorNovo()).isNull();
    }

    @Test
    void editar_removerResponsavelSemGerenciar_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), false);
        tarefa.setResponsavel(usuarioAutenticado);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, PERMISSAO_GERENCIAR);

        assertThatThrownBy(
                        () -> service.editar(tarefa.getId(), new EditarTarefaRequest(null, null, null, true)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void editar_projetoFinalizado_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(
                        () ->
                                service.editar(
                                        tarefa.getId(), new EditarTarefaRequest(null, null, UUID.randomUUID(), false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void editar_campoEstruturalComTarefaIniciada_lanca409() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.editar(
                                        tarefa.getId(), new EditarTarefaRequest("Novo título", null, null, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void editar_campoEstruturalComTarefaNaoIniciada_permiteEEditaComGerenciar() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(tarefa.getId(), new EditarTarefaRequest("Novo título", null, null, false));

        assertThat(tarefa.getTitulo()).isEqualTo("Novo título");
        verify(permissaoGuard).exigir(projetoId, PERMISSAO_GERENCIAR);
    }

    @Test
    void editar_devAtribuindoATerceiro_lanca403() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, false);
        UUID terceiroId = UUID.randomUUID();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                service.editar(
                                        tarefa.getId(), new EditarTarefaRequest(null, null, terceiroId, false)))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void editar_devAutoatribuindoTarefaJaAtribuidaOutro_permitido() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, false);
        Usuario outroResponsavel = new Usuario();
        outroResponsavel.setId(UUID.randomUUID());
        tarefa.setResponsavel(outroResponsavel);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(false);
        when(usuarioRepository.findById(usuarioAutenticado.getId())).thenReturn(Optional.of(usuarioAutenticado));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioAutenticado.getId(), projetoId))
                .thenReturn(List.of(new UsuarioProjetoPapel()));
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(tarefa.getId(), new EditarTarefaRequest(null, null, usuarioAutenticado.getId(), false));

        assertThat(tarefa.getResponsavel()).isEqualTo(usuarioAutenticado);
        var auditoriaCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditoriaCaptor.capture());
        assertThat(auditoriaCaptor.getValue().getCampo()).isEqualTo("responsavel");
        assertThat(auditoriaCaptor.getValue().getValorAnterior()).isEqualTo(outroResponsavel.getId().toString());
        assertThat(auditoriaCaptor.getValue().getValorNovo()).isEqualTo(usuarioAutenticado.getId().toString());
    }

    @Test
    void editar_gestorReatribuiLivremente_permitido() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, true);
        UUID novoResponsavelId = UUID.randomUUID();
        Usuario novoResponsavel = new Usuario();
        novoResponsavel.setId(novoResponsavelId);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(true);
        when(usuarioRepository.findById(novoResponsavelId)).thenReturn(Optional.of(novoResponsavel));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(novoResponsavelId, projetoId))
                .thenReturn(List.of(new UsuarioProjetoPapel()));
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.editar(tarefa.getId(), new EditarTarefaRequest(null, null, novoResponsavelId, false));

        assertThat(tarefa.getResponsavel()).isEqualTo(novoResponsavel);
    }

    @Test
    void editar_responsavelNaoVinculadoAoProjeto_lanca422() {
        Workflow workflow = workflow();
        Etapa etapa = etapaComWorkflow(0, workflow, false);
        Tarefa tarefa = tarefa(etapa, workflow, false);
        UUID novoResponsavelId = UUID.randomUUID();
        Usuario novoResponsavel = new Usuario();
        novoResponsavel.setId(novoResponsavelId);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(true);
        when(usuarioRepository.findById(novoResponsavelId)).thenReturn(Optional.of(novoResponsavel));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(novoResponsavelId, projetoId))
                .thenReturn(List.of());

        assertThatThrownBy(
                        () ->
                                service.editar(
                                        tarefa.getId(), new EditarTarefaRequest(null, null, novoResponsavelId, false)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(tarefaRepository, never()).save(any());
    }

    // ---- TASK-04.2: detalhe (lead-time) ----

    @Test
    void detalhe_calculaLeadTimePorEtapaIncluindoEtapaEmAndamento() {
        Workflow workflow = workflow();
        Etapa e0 = etapaComWorkflow(0, workflow, false);
        Etapa e1 = etapaComWorkflow(1, workflow, false);
        Tarefa tarefa = tarefa(e1, workflow, true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);

        OffsetDateTime entradaFechada = OffsetDateTime.now().minusHours(3);
        OffsetDateTime saidaFechada = entradaFechada.plusHours(1);
        TarefaEtapaHistorico fechado = new TarefaEtapaHistorico();
        fechado.setEtapa(e0);
        fechado.setEntradaEm(entradaFechada);
        fechado.setSaidaEm(saidaFechada);

        OffsetDateTime entradaAberta = OffsetDateTime.now().minusMinutes(30);
        TarefaEtapaHistorico aberto = new TarefaEtapaHistorico();
        aberto.setEtapa(e1);
        aberto.setEntradaEm(entradaAberta);
        aberto.setSaidaEm(null);

        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEm(tarefa.getId()))
                .thenReturn(List.of(fechado, aberto));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaId(tarefa.getId())).thenReturn(List.of());

        TarefaDetalheResponse response = service.detalhe(tarefa.getId());

        assertThat(response.historicoEtapas()).hasSize(2);
        assertThat(response.historicoEtapas().get(0).leadTimeSegundos()).isEqualTo(3600L);
        assertThat(response.historicoEtapas().get(1).saidaEm()).isNull();
        assertThat(response.historicoEtapas().get(1).leadTimeSegundos()).isGreaterThanOrEqualTo(1799L);
        assertThat(response.tempoImpedimentoTotalSegundos()).isZero();
    }

    @Test
    void detalhe_semPermissaoDeMembro_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), false);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        assertThatThrownBy(() -> service.detalhe(tarefa.getId())).isInstanceOf(AccessDeniedException.class);
    }

    // ---- TASK-04.3: impedimento ----

    @Test
    void marcarImpedimento_semPermissao_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, PERMISSAO_IMPEDIMENTO);

        assertThatThrownBy(() -> service.marcarImpedimento(tarefa.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaImpedimentoHistoricoRepository, never()).save(any());
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void marcarImpedimento_projetoFinalizado_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.marcarImpedimento(tarefa.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void marcarImpedimento_tarefaJaImpedida_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        tarefa.setImpedida(true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

        assertThatThrownBy(() -> service.marcarImpedimento(tarefa.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(tarefaImpedimentoHistoricoRepository, never()).save(any());
    }

    @Test
    void marcarImpedimento_abreHistoricoESetaImpedida() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.marcarImpedimento(tarefa.getId());

        assertThat(tarefa.isImpedida()).isTrue();
        assertThat(tarefa.getImpedidaDesde()).isNotNull();
        var historicoCaptor = ArgumentCaptor.forClass(TarefaImpedimentoHistorico.class);
        verify(tarefaImpedimentoHistoricoRepository).save(historicoCaptor.capture());
        assertThat(historicoCaptor.getValue().getMarcadoEm()).isNotNull();
        assertThat(historicoCaptor.getValue().getDesmarcadoEm()).isNull();
        var auditoriaCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditoriaCaptor.capture());
        assertThat(auditoriaCaptor.getValue().getCampo()).isEqualTo("impedimento");
        verify(notificacaoService)
                .notificarObservadores(
                        org.mockito.ArgumentMatchers.eq(tarefa),
                        org.mockito.ArgumentMatchers.eq(
                                com.crudao.kanban.notificacao.NotificacaoService.TIPO_IMPEDIMENTO_MARCADO),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void desmarcarImpedimento_semPermissao_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        tarefa.setImpedida(true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, PERMISSAO_IMPEDIMENTO);

        assertThatThrownBy(() -> service.desmarcarImpedimento(tarefa.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).save(any());
    }

    @Test
    void desmarcarImpedimento_tarefaNaoImpedida_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

        assertThatThrownBy(() -> service.desmarcarImpedimento(tarefa.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void desmarcarImpedimento_fechaHistoricoAbertoESetaLivre() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        tarefa.setImpedida(true);
        tarefa.setImpedidaDesde(OffsetDateTime.now().minusHours(2));
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        TarefaImpedimentoHistorico aberto = new TarefaImpedimentoHistorico();
        aberto.setTarefa(tarefa);
        aberto.setMarcadoEm(tarefa.getImpedidaDesde());
        TarefaImpedimentoHistorico fechadoAnterior = new TarefaImpedimentoHistorico();
        fechadoAnterior.setTarefa(tarefa);
        fechadoAnterior.setMarcadoEm(OffsetDateTime.now().minusDays(1));
        fechadoAnterior.setDesmarcadoEm(OffsetDateTime.now().minusDays(1).plusMinutes(10));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaId(tarefa.getId()))
                .thenReturn(List.of(fechadoAnterior, aberto));
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desmarcarImpedimento(tarefa.getId());

        assertThat(tarefa.isImpedida()).isFalse();
        assertThat(tarefa.getImpedidaDesde()).isNull();
        assertThat(aberto.getDesmarcadoEm()).isNotNull();
        assertThat(fechadoAnterior.getDesmarcadoEm()).isNotNull();
        var auditoriaCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditoriaCaptor.capture());
        assertThat(auditoriaCaptor.getValue().getCampo()).isEqualTo("impedimento");
        verify(notificacaoService)
                .notificarObservadores(
                        org.mockito.ArgumentMatchers.eq(tarefa),
                        org.mockito.ArgumentMatchers.eq(
                                com.crudao.kanban.notificacao.NotificacaoService.TIPO_IMPEDIMENTO_DESMARCADO),
                        org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void desmarcarImpedimento_projetoFinalizado_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        tarefa.setImpedida(true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.desmarcarImpedimento(tarefa.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void impedimento_multiplosCiclos_acumulamTempoCorretamente() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(tarefaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1º ciclo: marca e desmarca 10 min depois.
        service.marcarImpedimento(tarefa.getId());
        TarefaImpedimentoHistorico ciclo1 = new TarefaImpedimentoHistorico();
        ciclo1.setTarefa(tarefa);
        ciclo1.setMarcadoEm(OffsetDateTime.now().minusMinutes(30));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaId(tarefa.getId())).thenReturn(List.of(ciclo1));
        service.desmarcarImpedimento(tarefa.getId());
        assertThat(tarefa.isImpedida()).isFalse();
        assertThat(ciclo1.getDesmarcadoEm()).isNotNull();

        // 2º ciclo: marca de novo e desmarca 5 min depois — histórico acumula os dois fechados.
        service.marcarImpedimento(tarefa.getId());
        TarefaImpedimentoHistorico ciclo2 = new TarefaImpedimentoHistorico();
        ciclo2.setTarefa(tarefa);
        ciclo2.setMarcadoEm(OffsetDateTime.now().minusMinutes(10));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaId(tarefa.getId()))
                .thenReturn(List.of(ciclo1, ciclo2));
        service.desmarcarImpedimento(tarefa.getId());

        assertThat(tarefa.isImpedida()).isFalse();
        assertThat(ciclo2.getDesmarcadoEm()).isNotNull();
        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEm(tarefa.getId())).thenReturn(List.of());
        when(permissaoGuard.membro(projetoId)).thenReturn(true);

        TarefaDetalheResponse detalhe = service.detalhe(tarefa.getId());
        long ciclo1Segundos = Duration.between(ciclo1.getMarcadoEm(), ciclo1.getDesmarcadoEm()).getSeconds();
        long ciclo2Segundos = Duration.between(ciclo2.getMarcadoEm(), ciclo2.getDesmarcadoEm()).getSeconds();
        assertThat(detalhe.tempoImpedimentoTotalSegundos()).isEqualTo(ciclo1Segundos + ciclo2Segundos);
    }

    @Test
    void impedimento_tarefaInexistente_lanca404() {
        UUID id = UUID.randomUUID();
        when(tarefaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarImpedimento(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ---- TASK-04.4: excluir ----

    @Test
    void excluir_semPermissaoGerenciar_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, PERMISSAO_GERENCIAR);

        assertThatThrownBy(() -> service.excluir(tarefa.getId())).isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).delete(any());
    }

    @Test
    void excluir_projetoFinalizado_lanca409() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado")).when(permissaoGuard).exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.excluir(tarefa.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(tarefaRepository, never()).delete(any());
    }

    @Test
    void excluir_devSemTogglePermissaoExcluir_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        UsuarioProjetoPapel vinculoDev = new UsuarioProjetoPapel();
        Papel papelDev = new Papel();
        papelDev.setChave("dev");
        vinculoDev.setPapel(papelDev);
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioAutenticado.getId(), projetoId))
                .thenReturn(List.of(vinculoDev));
        doNothing().when(permissaoGuard).exigir(projetoId, PERMISSAO_GERENCIAR);
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "tarefa:excluir");

        assertThatThrownBy(() -> service.excluir(tarefa.getId())).isInstanceOf(AccessDeniedException.class);
        verify(tarefaRepository, never()).delete(any());
    }

    @Test
    void excluir_devComTogglePermissaoExcluir_permitido() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        UsuarioProjetoPapel vinculoDev = new UsuarioProjetoPapel();
        Papel papelDev = new Papel();
        papelDev.setChave("dev");
        vinculoDev.setPapel(papelDev);
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioAutenticado.getId(), projetoId))
                .thenReturn(List.of(vinculoDev));

        service.excluir(tarefa.getId());

        verify(tarefaEtapaHistoricoRepository).deleteByTarefaId(tarefa.getId());
        verify(tarefaImpedimentoHistoricoRepository).deleteByTarefaId(tarefa.getId());
        verify(tarefaObservadorRepository).deleteByTarefaId(tarefa.getId());
        verify(tarefaAuditoriaRepository).deleteByTarefaId(tarefa.getId());
        verify(notificacaoRepository).deleteByTarefaId(tarefa.getId());
        verify(tarefaRepository).delete(tarefa);
        verify(eventoBoardPublisher)
                .publicar(projetoId, com.crudao.kanban.evento.TipoEventoBoard.TAREFA_EXCLUIDA, tarefa.getId());
    }

    @Test
    void excluir_gestorSemVinculoDev_naoExigeTogglePermissaoExcluir() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioAutenticado.getId(), projetoId))
                .thenReturn(List.of());

        service.excluir(tarefa.getId());

        verify(permissaoGuard, never()).exigir(projetoId, "tarefa:excluir");
        verify(tarefaRepository).delete(tarefa);
    }

    @Test
    void excluir_tarefaInexistente_lanca404() {
        UUID id = UUID.randomUUID();
        when(tarefaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ---- TASK-04.4: auditoria ----

    @Test
    void auditoria_semPermissaoDedicada_lanca403() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "tarefa:auditoria");

        assertThatThrownBy(() -> service.auditoria(tarefa.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void auditoria_semVinculoAoProjeto_lanca403() {
        // IDOR cross-projeto: usuário sem nenhum vínculo (não é gestor/admin de nenhum projeto)
        // não pode ler a auditoria de uma tarefa de projeto ao qual não pertence.
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "tarefa:auditoria");

        assertThatThrownBy(() -> service.auditoria(tarefa.getId())).isInstanceOf(AccessDeniedException.class);
        verify(tarefaAuditoriaRepository, never()).findByTarefaIdOrderByDataHora(any());
    }

    @Test
    void auditoria_retornaHistoricoComTodosOsCamposRelevantes() {
        Etapa etapa = etapaComWorkflow(0, workflow(), false);
        Tarefa tarefa = tarefa(etapa, workflow(), true);
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));

        TarefaAuditoria registroTitulo = new TarefaAuditoria();
        registroTitulo.setAutor(usuarioAutenticado);
        registroTitulo.setCampo("titulo");
        registroTitulo.setValorAnterior("Antigo");
        registroTitulo.setValorNovo("Novo");
        registroTitulo.setDataHora(OffsetDateTime.now().minusHours(3));

        TarefaAuditoria registroResponsavel = new TarefaAuditoria();
        registroResponsavel.setAutor(usuarioAutenticado);
        registroResponsavel.setCampo("responsavel");
        registroResponsavel.setValorAnterior(null);
        registroResponsavel.setValorNovo(UUID.randomUUID().toString());
        registroResponsavel.setDataHora(OffsetDateTime.now().minusHours(2));

        TarefaAuditoria registroEtapa = new TarefaAuditoria();
        registroEtapa.setAutor(usuarioAutenticado);
        registroEtapa.setCampo("etapa");
        registroEtapa.setValorAnterior(UUID.randomUUID().toString());
        registroEtapa.setValorNovo(UUID.randomUUID().toString());
        registroEtapa.setDataHora(OffsetDateTime.now().minusHours(1));

        TarefaAuditoria registroImpedimento = new TarefaAuditoria();
        registroImpedimento.setAutor(usuarioAutenticado);
        registroImpedimento.setCampo("impedimento");
        registroImpedimento.setValorAnterior(null);
        registroImpedimento.setValorNovo("marcado");
        registroImpedimento.setDataHora(OffsetDateTime.now());

        when(tarefaAuditoriaRepository.findByTarefaIdOrderByDataHora(tarefa.getId()))
                .thenReturn(List.of(registroTitulo, registroResponsavel, registroEtapa, registroImpedimento));

        List<TarefaAuditoriaResponse> resultado = service.auditoria(tarefa.getId());

        assertThat(resultado).hasSize(4);
        assertThat(resultado).extracting(TarefaAuditoriaResponse::campo)
                .containsExactly("titulo", "responsavel", "etapa", "impedimento");
        assertThat(resultado.get(0).autorId()).isEqualTo(usuarioAutenticado.getId());
        assertThat(resultado.get(0).valorAnterior()).isEqualTo("Antigo");
        assertThat(resultado.get(0).valorNovo()).isEqualTo("Novo");
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        return usuario;
    }

    private Projeto projeto() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        return projeto;
    }

    private Workflow workflow() {
        Workflow workflow = new Workflow();
        workflow.setId(workflowId);
        return workflow;
    }

    private Etapa etapa(int ordem) {
        Etapa etapa = new Etapa();
        etapa.setId(UUID.randomUUID());
        etapa.setOrdem(ordem);
        return etapa;
    }

    private Raia raia(Projeto projeto) {
        Raia raia = new Raia();
        raia.setId(UUID.randomUUID());
        raia.setProjeto(projeto);
        return raia;
    }

    private Etapa etapaComWorkflow(int ordem, Workflow workflow, boolean etapaFinal) {
        Etapa etapa = new Etapa();
        etapa.setId(UUID.randomUUID());
        etapa.setWorkflow(workflow);
        etapa.setOrdem(ordem);
        etapa.setEtapaFinal(etapaFinal);
        return etapa;
    }

    private Transicao transicao(Etapa origem, Etapa destino) {
        Transicao transicao = new Transicao();
        transicao.setId(UUID.randomUUID());
        transicao.setEtapaOrigem(origem);
        transicao.setEtapaDestino(destino);
        return transicao;
    }

    private Tarefa tarefa(Etapa etapaAtual, Workflow workflow, boolean iniciada) {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        tarefa.setProjeto(projeto());
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapaAtual);
        tarefa.setRaia(raia(projeto()));
        tarefa.setTitulo("Tarefa existente");
        tarefa.setIniciada(iniciada);
        tarefa.setCriadoPor(usuarioAutenticado);
        tarefa.setCriadoEm(OffsetDateTime.now());
        tarefa.setAtualizadoEm(OffsetDateTime.now());
        return tarefa;
    }
}
