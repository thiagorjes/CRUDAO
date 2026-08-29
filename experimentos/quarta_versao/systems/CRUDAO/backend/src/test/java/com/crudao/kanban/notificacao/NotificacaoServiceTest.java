package com.crudao.kanban.notificacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.notificacao.TipoNotificacao;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservador;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.evento.NotificacaoEventPublisher;
import com.crudao.kanban.evento.NotificacaoEventPublisher.NotificacaoEventPayload;
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
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TASK-05.2: Testes unitários para NotificacaoService.
 * RF-005: Notificações internas para observadores de tarefas.
 * RNF-003: Validação de autorização em endpoints.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoService - Testes Unitários")
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private TarefaObservadorRepository tarefaObservadorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacaoEventPublisher eventPublisher;

    @InjectMocks
    private NotificacaoService notificacaoService;

    private ObjectMapper objectMapper;
    private UUID projetoId;
    private UUID tarefaId;
    private Projeto projeto;
    private Usuario responsavel;
    private Usuario criador;
    private Usuario observadorExplicito;
    private Tarefa tarefa;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Injetar ObjectMapper manualmente (não mocado)
        notificacaoService = new NotificacaoService(
            notificacaoRepository,
            tarefaObservadorRepository,
            usuarioRepository,
            eventPublisher,
            objectMapper
        );

        projetoId = UUID.randomUUID();
        tarefaId = UUID.randomUUID();

        projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto Test");

        responsavel = new Usuario();
        responsavel.setId(UUID.randomUUID());
        responsavel.setEmail("responsavel@test.com");
        responsavel.setAtivo(true);

        criador = new Usuario();
        criador.setId(UUID.randomUUID());
        criador.setEmail("criador@test.com");
        criador.setAtivo(true);

        observadorExplicito = new Usuario();
        observadorExplicito.setId(UUID.randomUUID());
        observadorExplicito.setEmail("observador@test.com");
        observadorExplicito.setAtivo(true);

        tarefa = new Tarefa();
        tarefa.setId(tarefaId);
        tarefa.setProjeto(projeto);
        tarefa.setTitulo("Implementar autenticação");
        tarefa.setResponsavel(responsavel);
        tarefa.setCriadoPor(criador);

        // NotificacaoService resolve cada observador via usuarioRepository.findById (code-review
        // TASK-05.2, I1). Mapeia os ids conhecidos; ids desconhecidos caem num Usuario genérico.
        Map<UUID, Usuario> porId = new HashMap<>();
        porId.put(responsavel.getId(), responsavel);
        porId.put(criador.getId(), criador);
        porId.put(observadorExplicito.getId(), observadorExplicito);
        lenient()
                .when(usuarioRepository.findById(any(UUID.class)))
                .thenAnswer(
                        inv -> {
                            UUID id = inv.getArgument(0);
                            Usuario u = porId.get(id);
                            if (u == null) {
                                u = new Usuario();
                                u.setId(id);
                                u.setEmail(id + "@test.com");
                                u.setAtivo(true);
                            }
                            return Optional.of(u);
                        });
    }

    @AfterEach
    void tearDown() {
        reset(notificacaoRepository, tarefaObservadorRepository, usuarioRepository, eventPublisher);
    }

    // ==================== Cenário 1: Resolução de Observadores ====================

    @Test
    @DisplayName("Resolver observadores: inclui responsável + criador + explícitos")
    void testResolverObservadores_ComTodosOsTipos() {
        // Arrange
        var observadorExplicitoEntity = new TarefaObservador();
        observadorExplicitoEntity.setUsuario(observadorExplicito);
        observadorExplicitoEntity.setTarefa(tarefa);

        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(List.of(observadorExplicitoEntity));

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(3)).save(captor.capture());

        Set<UUID> usuariosNotificados = new HashSet<>();
        captor.getAllValues().forEach(notif -> usuariosNotificados.add(notif.getUsuario().getId()));

        assertTrue(usuariosNotificados.contains(responsavel.getId()), "Responsável deve ser notificado");
        assertTrue(usuariosNotificados.contains(criador.getId()), "Criador deve ser notificado");
        assertTrue(usuariosNotificados.contains(observadorExplicito.getId()), "Observador explícito deve ser notificado");
    }

    @Test
    @DisplayName("Resolver observadores: deduplicar quando responsável = criador")
    void testResolverObservadores_DeduplicarResponsavelCriador() {
        // Arrange
        tarefa.setCriadoPor(responsavel); // Criador é o mesmo que responsável

        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert
        verify(notificacaoRepository, times(1)).save(any(Notificacao.class));
        // Apenas 1 notificação criada (não duplicada)
    }

    @Test
    @DisplayName("Resolver observadores: tarefa sem responsável (null)")
    void testResolverObservadores_SemResponsavel() {
        // Arrange
        tarefa.setResponsavel(null);

        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(1)).save(captor.capture());

        assertEquals(criador.getId(), captor.getValue().getUsuario().getId(), "Apenas criador deve ser notificado");
    }

    @Test
    @DisplayName("Resolver observadores: múltiplos observadores explícitos (sem duplicatas)")
    void testResolverObservadores_MultiplosExplicitos() {
        // Arrange
        Usuario obs2 = new Usuario();
        obs2.setId(UUID.randomUUID());
        obs2.setEmail("obs2@test.com");

        var obsEntity1 = new TarefaObservador();
        obsEntity1.setUsuario(observadorExplicito);

        var obsEntity2 = new TarefaObservador();
        obsEntity2.setUsuario(obs2);

        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(List.of(obsEntity1, obsEntity2));

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert: 4 notificações (responsável + criador + 2 explícitos)
        verify(notificacaoRepository, times(4)).save(any(Notificacao.class));
    }

    // ==================== Cenário 2: Criação por Transição de Etapa ====================

    @Test
    @DisplayName("Criar notificações por transição de etapa com tipo correto")
    void testCriarNotificacoesPorTransicaoEtapa_TipoCorreto() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        var etapaOrigemId = UUID.randomUUID();
        var etapaDestinoId = UUID.randomUUID();

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(tarefa, etapaOrigemId, etapaDestinoId);

        // Assert — responsável + criador
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(2)).save(captor.capture());

        captor.getAllValues().forEach(notificacao -> {
            assertEquals(TipoNotificacao.TRANSICAO_ETAPA, notificacao.getTipo());
            assertEquals(tarefaId, notificacao.getTarefa().getId());
            assertFalse(notificacao.isLida());
            assertNotNull(notificacao.getCriadoEm());
        });
    }

    @Test
    @DisplayName("Criar notificações por transição: evento publicado para cada observador")
    void testCriarNotificacoesPorTransicaoEtapa_EventoPublicado() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert
        ArgumentCaptor<NotificacaoEventPayload> captor =
            ArgumentCaptor.forClass(NotificacaoEventPayload.class);
        verify(eventPublisher, times(2)).publicar(captor.capture());
        // 2 eventos (responsável + criador)

        List<NotificacaoEventPayload> eventos = captor.getAllValues();
        eventos.forEach(e -> {
            assertEquals("TRANSICAO_ETAPA", e.tipo());
            assertEquals(tarefaId, e.tarefaId());
        });
    }

    @Test
    @DisplayName("Criar notificações por transição: falha de publicação não bloqueia transação")
    void testCriarNotificacoesPorTransicaoEtapa_FalhaDePublicacaoNaoBloqueia() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());
        doThrow(new RuntimeException("Erro ao publicar evento"))
            .when(eventPublisher).publicar(any());

        // Act & Assert (não deve lançar exceção)
        assertDoesNotThrow(() ->
            notificacaoService.criarNotificacoesPorTransicaoEtapa(
                tarefa, UUID.randomUUID(), UUID.randomUUID()
            )
        );

        verify(notificacaoRepository, times(2)).save(any(Notificacao.class));
    }

    // ==================== Cenário 3: Criação por Impedimento ====================

    @Test
    @DisplayName("Criar notificações por impedimento marcado com tipo correto")
    void testCriarNotificacoesPorImpedimentoMarcado_TipoCorreto() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorImpedimentoMarcado(tarefa);

        // Assert — responsável + criador
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(2)).save(captor.capture());

        captor.getAllValues()
            .forEach(n -> assertEquals(TipoNotificacao.IMPEDIMENTO_MARCADO, n.getTipo()));
    }

    @Test
    @DisplayName("Criar notificações por impedimento desmarcado com tipo correto")
    void testCriarNotificacoesPorImpedimentoDesmarcado_TipoCorreto() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorImpedimentoDesmarcado(tarefa);

        // Assert — responsável + criador
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(2)).save(captor.capture());

        captor.getAllValues()
            .forEach(n -> assertEquals(TipoNotificacao.IMPEDIMENTO_DESMARCADO, n.getTipo()));
    }

    @Test
    @DisplayName("Criar notificações por impedimento: evento publicado")
    void testCriarNotificacoesPorImpedimento_EventoPublicado() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorImpedimentoMarcado(tarefa);

        // Assert
        ArgumentCaptor<NotificacaoEventPayload> captor =
            ArgumentCaptor.forClass(NotificacaoEventPayload.class);
        verify(eventPublisher, times(2)).publicar(captor.capture());

        List<NotificacaoEventPayload> eventos = captor.getAllValues();
        eventos.forEach(e -> assertEquals("IMPEDIMENTO_MARCADO", e.tipo()));
    }

    // ==================== Cenário 5: Marcação como Lida ====================

    @Test
    @DisplayName("Marcar notificação como lida com autorização")
    void testMarcarComoLidaComAutorizacao_Sucesso() {
        // Arrange
        var notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setUsuario(responsavel);
        notificacao.setLida(false);

        when(notificacaoRepository.findById(notificacao.getId()))
            .thenReturn(Optional.of(notificacao));

        // Act
        notificacaoService.marcarComoLidaComAutorizacao(notificacao.getId(), responsavel.getId());

        // Assert
        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());

        assertTrue(captor.getValue().isLida());
        assertNotNull(captor.getValue().getLidoEm());
    }

    @Test
    @DisplayName("Marcar notificação como lida: rejeitar acesso não autorizado")
    void testMarcarComoLidaComAutorizacao_RejetarOutroUsuario() {
        // Arrange
        var notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setUsuario(responsavel);

        when(notificacaoRepository.findById(notificacao.getId()))
            .thenReturn(Optional.of(notificacao));

        UUID outroUsuarioId = UUID.randomUUID();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            notificacaoService.marcarComoLidaComAutorizacao(notificacao.getId(), outroUsuarioId)
        );

        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Marcar notificação como lida: notificação não encontrada")
    void testMarcarComoLidaComAutorizacao_NaoEncontrada() {
        // Arrange
        var notificacaoId = UUID.randomUUID();
        when(notificacaoRepository.findById(notificacaoId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            notificacaoService.marcarComoLidaComAutorizacao(notificacaoId, UUID.randomUUID())
        );
    }

    // ==================== Cenário 4: Obter Não Lidas ====================

    @Test
    @DisplayName("Obter notificações não lidas ordenadas por data DESC")
    void testObterNaoLidas_Sucesso() {
        // Arrange
        var notif1 = new Notificacao();
        notif1.setId(UUID.randomUUID());
        notif1.setLida(false);
        notif1.setCriadoEm(Instant.now().minusSeconds(100));

        var notif2 = new Notificacao();
        notif2.setId(UUID.randomUUID());
        notif2.setLida(false);
        notif2.setCriadoEm(Instant.now());

        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(responsavel.getId()))
            .thenReturn(List.of(notif2, notif1));

        // Act
        List<Notificacao> resultado = notificacaoService.obterNaoLidas(responsavel.getId());

        // Assert
        assertEquals(2, resultado.size());
        assertEquals(notif2.getId(), resultado.get(0).getId(), "Mais recente deve vir primeiro");
        assertEquals(notif1.getId(), resultado.get(1).getId());
    }

    @Test
    @DisplayName("Obter notificações não lidas: lista vazia quando nenhuma")
    void testObterNaoLidas_Vazia() {
        // Arrange
        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(responsavel.getId()))
            .thenReturn(Collections.emptyList());

        // Act
        List<Notificacao> resultado = notificacaoService.obterNaoLidas(responsavel.getId());

        // Assert
        assertTrue(resultado.isEmpty());
    }
}
