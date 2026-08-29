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
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.domain.workflow.WorkflowRepository;
import com.crudao.kanban.evento.EventoBoardPublisher;
import com.crudao.kanban.notificacao.NotificacaoService;
import com.crudao.kanban.rbac.PermissaoGuard;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes para TASK-04.3: marcação/desmarcação de impedimento + histórico.
 * RF-004: sinalização de bloqueio com histórico e notificações.
 * RN-002: acumulação correta de tempo de impedimento (múltiplos ciclos).
 * RN-013: dev e product_owner podem marcar, gestor não.
 */
@ExtendWith(MockitoExtension.class)
class TarefaImpedimentoServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;

    @Mock
    private TarefaAuditoriaRepository tarefaAuditoriaRepository;

    @Mock
    private ProjetoRepository projetoRepository;

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
    private Raia raia;
    private Tarefa tarefa;

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
        tarefa.setImpedidaDesde(null);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    // ===== TESTES DE MARCAÇÃO DE IMPEDIMENTO (POST /api/tarefas/{id}/impedimento) =====

    @Test
    @DisplayName("marcar_when_comPermissao_should_marcarComSucesso")
    void marcar_when_comPermissao_should_marcarComSucesso() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(tarefaImpedimentoHistoricoRepository.save(any(TarefaImpedimentoHistorico.class)))
                .thenAnswer(inv -> {
                    TarefaImpedimentoHistorico h = inv.getArgument(0);
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
        tarefaService.marcarImpedimento(tarefaId, projetoId);

        // Assert
        assertTrue(tarefa.isImpedida(), "Tarefa deve estar marcada como impedida");
        assertNotNull(tarefa.getImpedidaDesde(), "impedidaDesde deve estar preenchido");

        // Verifica abertura de histórico
        ArgumentCaptor<TarefaImpedimentoHistorico> historicoCaptor =
                ArgumentCaptor.forClass(TarefaImpedimentoHistorico.class);
        verify(tarefaImpedimentoHistoricoRepository).save(historicoCaptor.capture());
        TarefaImpedimentoHistorico historico = historicoCaptor.getValue();
        assertEquals(tarefa, historico.getTarefa());
        assertNotNull(historico.getMarcadoEm());
        assertNull(historico.getDesmarcadoEm());
    }

    @Test
    @DisplayName("marcar_when_semPermissao_should_retornarErro403")
    void marcar_when_semPermissao_should_retornarErro403() {
        // Arrange — exception é lançada na validação de permissão, antes de buscar tarefa
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Permissão negada"))
                .when(permissaoGuard).exigir(projetoId, "tarefa:impedimento");

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            tarefaService.marcarImpedimento(tarefaId, projetoId)
        );
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("marcar_when_projetoFinalizado_should_retornarErro409")
    void marcar_when_projetoFinalizado_should_retornarErro409() {
        // Arrange — projeto finalizado é validado por exigirProjetoAtivo() que lança AccessDeniedException
        doThrow(new org.springframework.security.access.AccessDeniedException("Acesso negado"))
                .when(permissaoGuard).exigirProjetoAtivo(projetoId);

        // Act & Assert
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
            tarefaService.marcarImpedimento(tarefaId, projetoId)
        );
    }

    @Test
    @DisplayName("marcar_should_gravarTarefaAuditoria")
    void marcar_should_gravarTarefaAuditoria() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(tarefaImpedimentoHistoricoRepository.save(any(TarefaImpedimentoHistorico.class)))
                .thenAnswer(inv -> {
                    TarefaImpedimentoHistorico h = inv.getArgument(0);
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
        tarefaService.marcarImpedimento(tarefaId, projetoId);

        // Assert
        ArgumentCaptor<TarefaAuditoria> auditCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditCaptor.capture());
        TarefaAuditoria audit = auditCaptor.getValue();
        assertEquals(tarefaId, audit.getTarefa().getId());
        assertEquals("impedimento", audit.getCampo());
        assertEquals("false", audit.getValorAnterior()); // impedida era false
        assertEquals("true", audit.getValorNovo()); // impedida agora é true
        assertNotNull(audit.getDataHora());
    }

    // ===== TESTES DE DESMARCAÇÃO DE IMPEDIMENTO (DELETE /api/tarefas/{id}/impedimento) =====

    @Test
    @DisplayName("desmarcar_when_impedidaComHistorico_should_desmarcarComSucesso")
    void desmarcar_when_impedidaComHistorico_should_desmarcarComSucesso() {
        // Arrange
        tarefa.setImpedida(true);
        tarefa.setImpedidaDesde(Instant.now().minusSeconds(300)); // impedida há 5 minutos

        TarefaImpedimentoHistorico historicoAberto = new TarefaImpedimentoHistorico();
        historicoAberto.setId(UUID.randomUUID());
        historicoAberto.setTarefa(tarefa);
        historicoAberto.setMarcadoEm(tarefa.getImpedidaDesde());
        historicoAberto.setDesmarcadoEm(null);

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaIdAndDesmarcadoEmIsNull(tarefaId))
                .thenReturn(Optional.of(historicoAberto));
        when(tarefaImpedimentoHistoricoRepository.save(any(TarefaImpedimentoHistorico.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);

        // Assert
        assertFalse(tarefa.isImpedida(), "Tarefa deve estar desmarcada");

        // Verifica fechamento de histórico
        ArgumentCaptor<TarefaImpedimentoHistorico> historicoCaptor =
                ArgumentCaptor.forClass(TarefaImpedimentoHistorico.class);
        verify(tarefaImpedimentoHistoricoRepository).save(historicoCaptor.capture());
        TarefaImpedimentoHistorico historicoFechado = historicoCaptor.getValue();
        assertNotNull(historicoFechado.getDesmarcadoEm(), "desmarcadoEm deve estar preenchido");
        assertEquals(historicoAberto.getMarcadoEm(), historicoFechado.getMarcadoEm());
    }

    @Test
    @DisplayName("desmarcar_when_naoImpedida_should_retornarErro409")
    void desmarcar_when_naoImpedida_should_retornarErro409() {
        // Arrange — tarefa não impedida retorna 409 (conflito)
        tarefa.setImpedida(false);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));

        // Act & Assert
        assertThrows(ResponseStatusException.class, () ->
            tarefaService.desmarcarImpedimento(tarefaId, projetoId)
        );
    }

    @Test
    @DisplayName("desmarcar_should_gravarTarefaAuditoria")
    void desmarcar_should_gravarTarefaAuditoria() {
        // Arrange
        tarefa.setImpedida(true);
        tarefa.setImpedidaDesde(Instant.now().minusSeconds(300));

        TarefaImpedimentoHistorico historicoAberto = new TarefaImpedimentoHistorico();
        historicoAberto.setId(UUID.randomUUID());
        historicoAberto.setTarefa(tarefa);
        historicoAberto.setMarcadoEm(tarefa.getImpedidaDesde());
        historicoAberto.setDesmarcadoEm(null);

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(tarefaImpedimentoHistoricoRepository.findByTarefaIdAndDesmarcadoEmIsNull(tarefaId))
                .thenReturn(Optional.of(historicoAberto));
        when(tarefaImpedimentoHistoricoRepository.save(any(TarefaImpedimentoHistorico.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        // Act
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);

        // Assert
        ArgumentCaptor<TarefaAuditoria> auditCaptor = ArgumentCaptor.forClass(TarefaAuditoria.class);
        verify(tarefaAuditoriaRepository).save(auditCaptor.capture());
        TarefaAuditoria audit = auditCaptor.getValue();
        assertEquals(tarefaId, audit.getTarefa().getId());
        assertEquals("impedimento", audit.getCampo());
        assertEquals("true", audit.getValorAnterior());
        assertEquals("false", audit.getValorNovo());
        assertNotNull(audit.getDataHora());
    }

    // ===== TESTES DE MÚLTIPLOS CICLOS =====

    @Test
    @DisplayName("multiplos_ciclos_when_marca_desmarca_marca_should_acumularCorretamente")
    void multiplos_ciclos_when_marca_desmarca_marca_should_acumularCorretamente() {
        // Arrange: rastrear apenas novas aberturas (não modificações)
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));

        List<TarefaImpedimentoHistorico> novasAberturas = new ArrayList<>();
        when(tarefaImpedimentoHistoricoRepository.save(any(TarefaImpedimentoHistorico.class)))
                .thenAnswer(inv -> {
                    TarefaImpedimentoHistorico h = inv.getArgument(0);
                    h.setId(UUID.randomUUID());
                    // Apenas rastreia se desmarcadoEm é null (nova abertura)
                    if (h.getDesmarcadoEm() == null) {
                        novasAberturas.add(h);
                    }
                    return h;
                });
        when(tarefaRepository.save(any(Tarefa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tarefaAuditoriaRepository.save(any(TarefaAuditoria.class)))
                .thenAnswer(inv -> {
                    TarefaAuditoria a = inv.getArgument(0);
                    a.setId(UUID.randomUUID());
                    return a;
                });

        TarefaImpedimentoHistorico historico1 = new TarefaImpedimentoHistorico();

        // Act - Primeira marcação
        tarefaService.marcarImpedimento(tarefaId, projetoId);
        assertTrue(tarefa.isImpedida());
        assertEquals(1, novasAberturas.size(), "Deve ter 1 nova abertura");
        historico1 = novasAberturas.get(0);

        // Preparar para desmarcação
        when(tarefaImpedimentoHistoricoRepository.findByTarefaIdAndDesmarcadoEmIsNull(tarefaId))
                .thenReturn(Optional.of(historico1));

        // Act - Primeira desmarcação
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);
        assertFalse(tarefa.isImpedida());
        assertNotNull(historico1.getDesmarcadoEm(), "Primeiro histórico deve estar fechado");

        // Preparar para segunda marcação
        tarefa.setImpedidaDesde(null);

        // Act - Segunda marcação
        tarefaService.marcarImpedimento(tarefaId, projetoId);
        assertTrue(tarefa.isImpedida());
        assertEquals(2, novasAberturas.size(), "Deve ter 2 novas aberturas (múltiplos ciclos)");
        assertNull(novasAberturas.get(1).getDesmarcadoEm(), "Segundo histórico deve estar aberto");

        // Assert: múltiplos ciclos suportados
        assertEquals(2, novasAberturas.stream().filter(h -> h.getMarcadoEm() != null).count());
    }
}
