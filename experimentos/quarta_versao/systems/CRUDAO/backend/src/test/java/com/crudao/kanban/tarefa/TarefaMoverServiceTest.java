package com.crudao.kanban.tarefa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.raia.RaiaRepository;
import com.crudao.kanban.domain.tarefa.*;
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
import com.crudao.kanban.notificacao.NotificacaoService;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.crudao.kanban.tarefa.dto.EditarTarefaRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import java.time.Instant;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes para TASK-04.2: mover tarefa, congelamento pós-início, lead-time e autoatribuição (RN-012).
 */
@ExtendWith(MockitoExtension.class)
class TarefaMoverServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;

    @Mock
    private TarefaAuditoriaRepository tarefaAuditoriaRepository;

    @Mock
    private TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;

    @Mock
    private com.crudao.kanban.domain.tarefa.TarefaObservadorRepository tarefaObservadorRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private WorkflowRepository workflowRepository;

    @Mock
    private EtapaRepository etapaRepository;

    @Mock
    private TransicaoRepository transicaoRepository;

    @Mock
    private RaiaRepository raiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PermissaoGuard permissaoGuard;

    // Colaboradores adicionados ao TarefaService em TASK-05.1/05.2.
    @Mock
    private EventoBoardPublisher eventoBoardPublisher;

    @Mock
    private NotificacaoService notificacaoService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TarefaService tarefaService;

    private UUID projetoId;
    private UUID tarefaId;
    private Projeto projeto;
    private Usuario usuarioLogado;
    private Workflow workflow;
    private Etapa etapa1;
    private Etapa etapa2;
    private Etapa etapa3;
    private Raia raia;
    private Tarefa tarefa;
    private TarefaEtapaHistorico historicoEtapa1;

    @BeforeEach
    void setUp() {
        projetoId = UUID.randomUUID();
        tarefaId = UUID.randomUUID();

        projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto Test");
        projeto.setStatus(Projeto.Status.ATIVO);

        usuarioLogado = new Usuario();
        usuarioLogado.setId(UUID.randomUUID());
        usuarioLogado.setEmail("dev@test.com");
        usuarioLogado.setAtivo(true);
        UsuarioAutenticadoHolder.set(usuarioLogado);

        workflow = new Workflow(UUID.randomUUID(), projeto, "Standard Workflow");
        etapa1 = new Etapa(UUID.randomUUID(), workflow, "Backlog", 1, false);
        etapa2 = new Etapa(UUID.randomUUID(), workflow, "In Progress", 2, false);
        etapa3 = new Etapa(UUID.randomUUID(), workflow, "Done", 3, true);

        raia = new Raia(UUID.randomUUID(), projeto, "Frontend", 1);

        tarefa = new Tarefa();
        tarefa.setId(tarefaId);
        tarefa.setProjeto(projeto);
        tarefa.setWorkflow(workflow);
        tarefa.setEtapaAtual(etapa1);
        tarefa.setRaia(raia);
        tarefa.setTitulo("Implementar login");
        tarefa.setResponsavel(null);
        tarefa.setCriadoPor(usuarioLogado);
        tarefa.setIniciada(false);
        tarefa.setImpedida(false);

        historicoEtapa1 = new TarefaEtapaHistorico();
        historicoEtapa1.setTarefa(tarefa);
        historicoEtapa1.setEtapa(etapa1);
        historicoEtapa1.setEntradaEm(Instant.now());
        historicoEtapa1.setSaidaEm(null);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    // ===== TESTES DE MOVIMENTAÇÃO (POST /api/tarefas/{id}/mover) =====

    @Test
    @DisplayName("mover_when_transicaoConfiguradalready_should_movimentarComSucesso")
    void mover_when_transicaoConfigurada_should_movimentarComSucesso() {
        // Arrange: transição etapa1 → etapa2 existe
        Transicao transicao = new Transicao(UUID.randomUUID(), etapa1, etapa2);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(etapaRepository.findById(etapa2.getId())).thenReturn(Optional.of(etapa2));
        when(transicaoRepository.findByEtapaOrigemId(etapa1.getId())).thenReturn(List.of(transicao));

        // Mock do histórico de etapa
        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId))
                .thenReturn(List.of(historicoEtapa1));

        // Mocks para histórico
        when(tarefaEtapaHistoricoRepository.save(any(TarefaEtapaHistorico.class)))
                .thenAnswer(inv -> {
                    TarefaEtapaHistorico h = inv.getArgument(0);
                    h.setId(UUID.randomUUID());
                    return h;
                });
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        MoverTarefaRequest request = new MoverTarefaRequest(etapa2.getId());
        tarefaService.mover(tarefaId, request);

        // Assert
        verify(tarefaRepository).findById(tarefaId);
        verify(transicaoRepository).findByEtapaOrigemId(etapa1.getId());
        // Verifica que o histórico atual foi fechado
        ArgumentCaptor<TarefaEtapaHistorico> captor = ArgumentCaptor.forClass(TarefaEtapaHistorico.class);
        verify(tarefaEtapaHistoricoRepository, atLeastOnce()).save(captor.capture());
        // Deve ter pelo menos um com saidaEm != null (fechando o histórico)
        assertTrue(captor.getAllValues().stream().anyMatch(h -> h.getSaidaEm() != null));
        assertEquals(etapa2, tarefa.getEtapaAtual());
        assertTrue(tarefa.isIniciada()); // Saiu da 1ª etapa
    }

    @Test
    @DisplayName("mover_when_transicaoNaoConfigurada_should_retornarErro409")
    void mover_when_transicaoNaoConfigurada_should_retornarErro409() {
        // Arrange: sem transição etapa1 → etapa3
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(etapaRepository.findById(etapa3.getId())).thenReturn(Optional.of(etapa3));
        when(transicaoRepository.findByEtapaOrigemId(etapa1.getId())).thenReturn(Collections.emptyList());

        // Act & Assert
        MoverTarefaRequest request = new MoverTarefaRequest(etapa3.getId());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.mover(tarefaId, request)
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("mover_when_paraEtapaFinalSemPermissao_should_retornarErro403")
    void mover_when_paraEtapaFinalSemPermissao_should_retornarErro403() {
        // Arrange: transição para etapa final (etapaFinal=true) sem tarefa:finalizar
        Transicao transicao = new Transicao(UUID.randomUUID(), etapa1, etapa3);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(etapaRepository.findById(etapa3.getId())).thenReturn(Optional.of(etapa3));
        when(transicaoRepository.findByEtapaOrigemId(etapa1.getId())).thenReturn(List.of(transicao));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Permissão negada"))
                .when(permissaoGuard).exigir(projetoId, "tarefa:finalizar");

        // Act & Assert
        MoverTarefaRequest request = new MoverTarefaRequest(etapa3.getId());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.mover(tarefaId, request)
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("mover_when_deEtapaFinalSemPermissao_should_retornarErro403")
    void mover_when_deEtapaFinalSemPermissao_should_retornarErro403() {
        // Arrange: tarefa em etapa final, tenta "desfinalizá-la" sem tarefa:finalizar
        tarefa.setEtapaAtual(etapa3);
        Transicao transicao = new Transicao(UUID.randomUUID(), etapa3, etapa2);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(etapaRepository.findById(etapa2.getId())).thenReturn(Optional.of(etapa2));
        when(transicaoRepository.findByEtapaOrigemId(etapa3.getId())).thenReturn(List.of(transicao));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Permissão negada"))
                .when(permissaoGuard).exigir(projetoId, "tarefa:finalizar");

        // Act & Assert
        MoverTarefaRequest request = new MoverTarefaRequest(etapa2.getId());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.mover(tarefaId, request)
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("mover_when_saidaDaPrimeiraEtapa_should_marcarIniciada")
    void mover_when_saidaDaPrimeiraEtapa_should_marcarIniciada() {
        // Arrange
        Transicao transicao = new Transicao(UUID.randomUUID(), etapa1, etapa2);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(etapaRepository.findById(etapa2.getId())).thenReturn(Optional.of(etapa2));
        when(transicaoRepository.findByEtapaOrigemId(etapa1.getId())).thenReturn(List.of(transicao));
        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId))
                .thenReturn(List.of(historicoEtapa1));
        when(tarefaEtapaHistoricoRepository.save(any(TarefaEtapaHistorico.class)))
                .thenAnswer(inv -> {
                    TarefaEtapaHistorico h = inv.getArgument(0);
                    h.setId(UUID.randomUUID());
                    return h;
                });
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        MoverTarefaRequest request = new MoverTarefaRequest(etapa2.getId());
        tarefaService.mover(tarefaId, request);

        // Assert
        assertTrue(tarefa.isIniciada());
    }

    @Test
    @DisplayName("mover_should_gravarTarefaAuditoria")
    void mover_should_gravarTarefaAuditoria() {
        // Arrange
        Transicao transicao = new Transicao(UUID.randomUUID(), etapa1, etapa2);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(etapaRepository.findById(etapa2.getId())).thenReturn(Optional.of(etapa2));
        when(transicaoRepository.findByEtapaOrigemId(etapa1.getId())).thenReturn(List.of(transicao));
        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId))
                .thenReturn(List.of(historicoEtapa1));
        when(tarefaEtapaHistoricoRepository.save(any(TarefaEtapaHistorico.class)))
                .thenAnswer(inv -> {
                    TarefaEtapaHistorico h = inv.getArgument(0);
                    h.setId(UUID.randomUUID());
                    return h;
                });
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        MoverTarefaRequest request = new MoverTarefaRequest(etapa2.getId());
        tarefaService.mover(tarefaId, request);

        // Assert
        ArgumentCaptor<TarefaAuditoria> auditCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditCaptor.capture());
        TarefaAuditoria audit = auditCaptor.getValue();
        assertEquals(tarefaId, audit.getTarefa().getId());
        assertEquals("etapa", audit.getCampo());
        assertEquals(etapa1.getNome(), audit.getValorAnterior());
        assertEquals(etapa2.getNome(), audit.getValorNovo());
        assertNotNull(audit.getDataHora());
    }

    // ===== TESTES DE CONGELAMENTO (PUT /api/tarefas/{id}) =====

    @Test
    @DisplayName("editar_when_tarefaAindaNaoIniciada_should_permitirEditarTodosCampos")
    void editar_when_tarefaAindaNaoIniciada_should_permitirEditarTodosCampos() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        EditarTarefaRequest request = new EditarTarefaRequest();
        request.setTitulo("Novo titulo");
        request.setDescricaoEscopo("Nova descrição");
        tarefaService.editar(tarefaId, request);

        // Assert
        assertEquals("Novo titulo", tarefa.getTitulo());
        assertEquals("Nova descrição", tarefa.getDescricaoEscopo());
    }

    @Test
    @DisplayName("editar_when_tarefaJaIniciada_should_bloqueiarCamposEstruturais")
    void editar_when_tarefaJaIniciada_should_bloqueiarCamposEstruturais() {
        // Arrange
        tarefa.setIniciada(true);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));

        // Act & Assert
        EditarTarefaRequest request = new EditarTarefaRequest();
        request.setTitulo("Novo titulo");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.editar(tarefaId, request)
        );
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("editar_when_tarefaIniciada_should_permitirResponsavel")
    void editar_when_tarefaIniciada_should_permitirResponsavel() {
        // Arrange
        tarefa.setIniciada(true);
        Usuario novoResponsavel = new Usuario();
        novoResponsavel.setId(UUID.randomUUID());
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(usuarioRepository.findById(novoResponsavel.getId())).thenReturn(Optional.of(novoResponsavel));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        EditarTarefaRequest request = new EditarTarefaRequest();
        request.setResponsavelId(novoResponsavel.getId());
        tarefaService.editar(tarefaId, request);

        // Assert
        assertEquals(novoResponsavel, tarefa.getResponsavel());
    }

    @Test
    @DisplayName("editar_when_devTentaAtribuirAOutrem_should_retornarErro403")
    void editar_when_devTentaAtribuirAOutrem_should_retornarErro403() {
        // Arrange: usuário logado é dev, tenta atribuir a outro
        tarefa.setIniciada(true);
        Usuario outroUsuario = new Usuario();
        outroUsuario.setId(UUID.randomUUID());
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(usuarioRepository.findById(outroUsuario.getId())).thenReturn(Optional.of(outroUsuario));
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Dev só pode se autoatribuir"))
                .when(permissaoGuard).validarAutoatribuicaoRN012(projetoId, usuarioLogado.getId(), outroUsuario.getId());

        // Act & Assert
        EditarTarefaRequest request = new EditarTarefaRequest();
        request.setResponsavelId(outroUsuario.getId());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.editar(tarefaId, request)
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    // ===== TESTES DE LEAD-TIME (GET /api/tarefas/{id}) =====

    @Test
    @DisplayName("obterComLeadTime_should_calcularLeadTimeCorretamente")
    void obterComLeadTime_should_calcularLeadTimeCorretamente() {
        // Arrange
        Instant agora = Instant.now();
        Instant umMinutoAtras = agora.minusSeconds(60);

        historicoEtapa1.setEntradaEm(umMinutoAtras);
        historicoEtapa1.setSaidaEm(null); // etapa atual em andamento

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId))
                .thenReturn(List.of(historicoEtapa1));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId))
                .thenReturn(Collections.emptyList());

        // Act
        var detalhe = tarefaService.obterComLeadTime(tarefaId);

        // Assert
        assertNotNull(detalhe);
        assertTrue(detalhe.getHistoricoEtapas().size() > 0);
        // O lead-time da etapa atual deve ser aproximadamente 60 segundos (±5)
        var etapa1History = detalhe.getHistoricoEtapas().get(0);
        assertTrue(etapa1History.getLeadTimeSegundos() >= 55 && etapa1History.getLeadTimeSegundos() <= 65,
                "Lead-time esperado ~60s, obteve " + etapa1History.getLeadTimeSegundos());
    }

    @Test
    @DisplayName("obterComLeadTime_should_incluirTempoImpedimento")
    void obterComLeadTime_should_incluirTempoImpedimento() {
        // Arrange
        Instant agora = Instant.now();
        Instant duasHorasAtras = agora.minusSeconds(7200);

        historicoEtapa1.setEntradaEm(duasHorasAtras);
        historicoEtapa1.setSaidaEm(agora.minusSeconds(60)); // saiu há 1 min

        TarefaImpedimentoHistorico impedimento = new TarefaImpedimentoHistorico();
        impedimento.setTarefa(tarefa);
        impedimento.setMarcadoEm(duasHorasAtras.plusSeconds(600)); // 10 min após entrada
        impedimento.setDesmarcadoEm(agora.minusSeconds(600)); // desmarked 10 min antes de sair

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(tarefaEtapaHistoricoRepository.findByTarefaIdOrderByEntradaEmAsc(tarefaId))
                .thenReturn(List.of(historicoEtapa1));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaIdOrderByMarcadoEmAsc(tarefaId))
                .thenReturn(List.of(impedimento));

        // Act
        var detalhe = tarefaService.obterComLeadTime(tarefaId);

        // Assert
        long tempoImpedimentoEsperado = 6600; // (agora - 600) - (duasHoras + 600) = 7200 - 1200 = 6000 seg, aprox.
        assertTrue(detalhe.getTempoImpedimentoTotalSegundos() > 0, "Tempo de impedimento deve ser > 0");
    }

    // ===== TESTES DE GAPS IDENTIFICADOS (Complemento TASK-04.2) =====

    @Test
    @DisplayName("mover_when_projetoFinalizado_should_retornarErro403")
    void mover_when_projetoFinalizado_should_retornarErro403() {
        // Arrange: projeto finalizado
        projeto.setStatus(Projeto.Status.FINALIZADO);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Projeto finalizado"))
                .when(permissaoGuard).exigirProjetoAtivo(projetoId);

        // Act & Assert
        MoverTarefaRequest request = new MoverTarefaRequest(etapa2.getId());
        assertThrows(AccessDeniedException.class, () ->
            tarefaService.mover(tarefaId, request)
        );
        verify(permissaoGuard).exigirProjetoAtivo(projetoId);
    }

    @Test
    @DisplayName("editar_when_responsavelAlterado_should_gravarAuditoria")
    void editar_when_responsavelAlterado_should_gravarAuditoria() {
        // Arrange
        UUID novoResponsavelId = UUID.randomUUID();
        Usuario novoResponsavel = new Usuario();
        novoResponsavel.setId(novoResponsavelId);

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(usuarioRepository.findById(novoResponsavelId)).thenReturn(Optional.of(novoResponsavel));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        EditarTarefaRequest request = new EditarTarefaRequest();
        request.setResponsavelId(novoResponsavelId);
        tarefaService.editar(tarefaId, request);

        // Assert
        ArgumentCaptor<TarefaAuditoria> auditCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditCaptor.capture());
        TarefaAuditoria audit = auditCaptor.getValue();
        assertEquals(tarefaId, audit.getTarefa().getId());
        assertEquals("responsavel", audit.getCampo());
        assertNull(audit.getValorAnterior()); // responsavel anterior era null
        assertEquals(novoResponsavelId.toString(), audit.getValorNovo());
        assertNotNull(audit.getDataHora());
    }

    @Test
    @DisplayName("editar_when_productOwnerAtribuiAOutrem_should_permitir")
    void editar_when_productOwnerAtribuiAOutrem_should_permitir() {
        // Arrange: product_owner pode atribuir a qualquer um (RN-012)
        // Simular que o usuário logado é product_owner (tem permissão tarefa:atribuir)
        UUID outroUsuarioId = UUID.randomUUID();
        Usuario outroUsuario = new Usuario();
        outroUsuario.setId(outroUsuarioId);

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(usuarioRepository.findById(outroUsuarioId)).thenReturn(Optional.of(outroUsuario));
        // Mock que product_owner tem tarefa:atribuir (ou dev sem tarefa:atribuir é rejeitado)
        // Neste teste, permitimos porque é product_owner
        doNothing().when(permissaoGuard).validarAutoatribuicaoRN012(projetoId, usuarioLogado.getId(), outroUsuarioId);
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        EditarTarefaRequest request = new EditarTarefaRequest();
        request.setResponsavelId(outroUsuarioId);
        tarefaService.editar(tarefaId, request);

        // Assert: nenhuma exceção lançada, responsável alterado
        assertEquals(outroUsuario, tarefa.getResponsavel());
        verify(tarefaAuditoriaRepository).save(any(TarefaAuditoria.class));
    }
}
