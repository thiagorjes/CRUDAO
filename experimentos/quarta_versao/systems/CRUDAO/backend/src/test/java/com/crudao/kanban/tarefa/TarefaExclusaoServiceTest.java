package com.crudao.kanban.tarefa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.tarefa.*;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.Workflow;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Testes para TASK-04.4: exclusão de tarefa + leitura de auditoria.
 * RF-019: exclusão de card pelo board.
 * RF-017: leitura de histórico de auditoria.
 * RN-CB-001: requer `tarefa:gerenciar`.
 * RN-CB-002: se dev, requer adicionalmente `tarefa:excluir` habilitada.
 * RN-CB-003: bloqueado se projeto finalizado.
 */
@ExtendWith(MockitoExtension.class)
class TarefaExclusaoServiceTest {

    @Mock
    private TarefaRepository tarefaRepository;

    @Mock
    private TarefaAuditoriaRepository tarefaAuditoriaRepository;

    @Mock
    private TarefaEtapaHistoricoRepository tarefaEtapaHistoricoRepository;

    @Mock
    private TarefaImpedimentoHistoricoRepository tarefaImpedimentoHistoricoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PapelRepository papelRepository;

    @Mock
    private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;

    @Mock
    private PermissaoGuard permissaoGuard;

    @InjectMocks
    private TarefaService tarefaService;

    private UUID projetoId;
    private UUID tarefaId;
    private Projeto projeto;
    private Usuario usuarioLogado;
    private Usuario usuarioDev;
    private Usuario usuarioProductOwner;
    private Workflow workflow;
    private Etapa etapa1;
    private Raia raia;
    private Tarefa tarefa;
    private Papel papelDev;
    private Papel papelProductOwner;

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

        usuarioDev = usuarioLogado;

        usuarioProductOwner = new Usuario();
        usuarioProductOwner.setId(UUID.randomUUID());
        usuarioProductOwner.setEmail("po@test.com");
        usuarioProductOwner.setAtivo(true);

        papelDev = new Papel();
        papelDev.setId(UUID.randomUUID());
        papelDev.setNome("dev");

        papelProductOwner = new Papel();
        papelProductOwner.setId(UUID.randomUUID());
        papelProductOwner.setNome("product_owner");

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
        tarefa.setCriadoEm(Instant.now().minusSeconds(3600));
        tarefa.setAtualizadoEm(Instant.now());
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    // ===== TESTES DE EXCLUSÃO =====

    @Test
    @DisplayName("excluir_when_devComToggleHabilitado_should_excluirComSucesso")
    void excluir_when_devComToggleHabilitado_should_excluirComSucesso() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));

        // Act
        tarefaService.excluirTarefa(tarefaId, projetoId);

        // Assert
        verify(tarefaRepository).deleteById(tarefaId);
        // Publicar evento seria aqui em caso de implementação real
    }

    @Test
    @DisplayName("excluir_when_semToggleTarefaExcluir_should_retornar403")
    void excluir_when_semToggleTarefaExcluir_should_retornar403() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard).exigirPermissaoExcluir(projetoId);

        // Act & Assert
        assertThrows(
                AccessDeniedException.class,
                () -> tarefaService.excluirTarefa(tarefaId, projetoId)
        );
    }

    @Test
    @DisplayName("excluir_when_projetoFinalizado_should_retornar409")
    void excluir_when_projetoFinalizado_should_retornar409() {
        // Arrange
        projeto.setStatus(Projeto.Status.FINALIZADO);
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Projeto finalizado"))
                .when(permissaoGuard).exigirProjetoAtivo(projetoId);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tarefaService.excluirTarefa(tarefaId, projetoId)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    @DisplayName("excluir_when_tarefaNaoExiste_should_retornar404")
    void excluir_when_tarefaNaoExiste_should_retornar404() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tarefaService.excluirTarefa(tarefaId, projetoId)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ===== TESTES DE AUDITORIA =====

    @Test
    @DisplayName("obterAuditoria_should_retornarHistoricoCompleto")
    void obterAuditoria_should_retornarHistoricoCompleto() {
        // Arrange
        TarefaAuditoria auditoria1 = new TarefaAuditoria();
        auditoria1.setId(UUID.randomUUID());
        auditoria1.setTarefa(tarefa);
        auditoria1.setAutor(usuarioLogado);
        auditoria1.setCampo("titulo");
        auditoria1.setValorAnterior("Velho título");
        auditoria1.setValorNovo("Novo título");
        auditoria1.setDataHora(Instant.now().minusSeconds(300));

        TarefaAuditoria auditoria2 = new TarefaAuditoria();
        auditoria2.setId(UUID.randomUUID());
        auditoria2.setTarefa(tarefa);
        auditoria2.setAutor(usuarioProductOwner);
        auditoria2.setCampo("responsavel");
        auditoria2.setValorAnterior(null);
        auditoria2.setValorNovo(usuarioDev.getId().toString());
        auditoria2.setDataHora(Instant.now().minusSeconds(100));

        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(tarefaAuditoriaRepository.findByTarefaIdOrderByDataHoraAsc(tarefaId))
                .thenReturn(List.of(auditoria1, auditoria2));

        // Act
        List<TarefaAuditoria> resultado = tarefaService.obterAuditoria(tarefaId);

        // Assert
        assertEquals(2, resultado.size());
        assertEquals("titulo", resultado.get(0).getCampo());
        assertEquals("responsavel", resultado.get(1).getCampo());
        assertEquals(auditoria1.getValorAnterior(), resultado.get(0).getValorAnterior());
    }

    @Test
    @DisplayName("obterAuditoria_when_tarefaNaoExiste_should_retornar404")
    void obterAuditoria_when_tarefaNaoExiste_should_retornar404() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tarefaService.obterAuditoria(tarefaId)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("obterAuditoria_when_empty_should_retornarListaVazia")
    void obterAuditoria_when_empty_should_retornarListaVazia() {
        // Arrange
        when(tarefaRepository.findById(tarefaId)).thenReturn(Optional.of(tarefa));
        when(tarefaAuditoriaRepository.findByTarefaIdOrderByDataHoraAsc(tarefaId))
                .thenReturn(Collections.emptyList());

        // Act
        List<TarefaAuditoria> resultado = tarefaService.obterAuditoria(tarefaId);

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
