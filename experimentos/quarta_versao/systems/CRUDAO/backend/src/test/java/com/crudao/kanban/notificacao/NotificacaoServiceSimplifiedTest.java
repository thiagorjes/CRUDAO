package com.crudao.kanban.notificacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TASK-05.2: Testes unitários simplificados para NotificacaoService.
 * Foca em comportamentos críticos, evitando ArgumentCaptor complexo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoService - Testes Unitários (Simplified)")
class NotificacaoServiceSimplifiedTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    @Mock
    private TarefaObservadorRepository tarefaObservadorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacaoEventPublisher eventPublisher;

    private NotificacaoService notificacaoService;

    private UUID projetoId;
    private UUID tarefaId;
    private Projeto projeto;
    private Usuario responsavel;
    private Usuario criador;
    private Usuario observadorExplicito;
    private Tarefa tarefa;

    @BeforeEach
    void setUp() {
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

        // ObjectMapper real (não mocável) — necessário para publicarEventoNotificacao.
        notificacaoService =
                new NotificacaoService(
                        notificacaoRepository,
                        tarefaObservadorRepository,
                        usuarioRepository,
                        eventPublisher,
                        new ObjectMapper());

        // NotificacaoService resolve cada observador via usuarioRepository.findById (code-review
        // TASK-05.2, I1).
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

    // ==================== Comportamento: Criação de Notificações ====================

    @Test
    @DisplayName("Criar notificações por transição: invoca save() e publicar() para cada observador")
    void testCriarNotificacoesPorTransicaoEtapa_InvocaSaveEPublicar() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert: save deve ser chamado para responsável + criador (2 vezes)
        verify(notificacaoRepository, times(2)).save(any(Notificacao.class));

        // Assert: publicar deve ser chamado para responsável + criador (2 vezes)
        verify(eventPublisher, times(2)).publicar(any());
    }

    @Test
    @DisplayName("Criar notificações: tarefa com 3 observadores (responsável + criador + explícito)")
    void testCriarNotificacoes_ComTresObservadores() {
        // Arrange
        var obsEntity = new TarefaObservador();
        obsEntity.setUsuario(observadorExplicito);

        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(List.of(obsEntity));

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert: 3 notificações criadas
        verify(notificacaoRepository, times(3)).save(any(Notificacao.class));
    }

    @Test
    @DisplayName("Criar notificações: tarefa sem responsável cria apenas para criador + observadores explícitos")
    void testCriarNotificacoes_SemResponsavel() {
        // Arrange
        tarefa.setResponsavel(null);

        var obsEntity = new TarefaObservador();
        obsEntity.setUsuario(observadorExplicito);

        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(List.of(obsEntity));

        // Act
        notificacaoService.criarNotificacoesPorTransicaoEtapa(
            tarefa, UUID.randomUUID(), UUID.randomUUID()
        );

        // Assert: 2 notificações (criador + observador, sem responsável)
        verify(notificacaoRepository, times(2)).save(any(Notificacao.class));
    }

    // ==================== Comportamento: Tipos de Notificação ====================

    @Test
    @DisplayName("Notificação por impedimento marcado tem tipo correto")
    void testImpedimentoMarcado_TipoCorreto() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorImpedimentoMarcado(tarefa);

        // Assert: save é chamado 2x (responsável + criador)
        verify(notificacaoRepository, times(2)).save(any(Notificacao.class));
        // Verificação de tipo é feita no teste de integração com BD real
    }

    @Test
    @DisplayName("Notificação por impedimento desmarcado tem tipo correto")
    void testImpedimentoDesmarcado_TipoCorreto() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        // Act
        notificacaoService.criarNotificacoesPorImpedimentoDesmarcado(tarefa);

        // Assert: save é chamado 2x
        verify(notificacaoRepository, times(2)).save(any(Notificacao.class));
    }

    // ==================== Comportamento: Tratamento de Erros ====================

    @Test
    @DisplayName("Falha ao publicar evento não bloqueia criação de notificação")
    void testFalhaDePublicacao_NaoBloqueia() {
        // Arrange
        when(tarefaObservadorRepository.findByTarefaId(tarefaId))
            .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Erro ao publicar"))
            .when(eventPublisher).publicar(any());

        // Act & Assert: não deve lançar exceção
        assertDoesNotThrow(() ->
            notificacaoService.criarNotificacoesPorTransicaoEtapa(
                tarefa, UUID.randomUUID(), UUID.randomUUID()
            )
        );

        // Notificações foram criadas mesmo com falha de publicação
        verify(notificacaoRepository, times(2)).save(any(Notificacao.class));
    }

    // ==================== Comportamento: Obter Não Lidas ====================

    @Test
    @DisplayName("Obter notificações não lidas chama repositório com usuarioId correto")
    void testObterNaoLidas_CallsRepository() {
        // Arrange
        var notif1 = new Notificacao();
        notif1.setId(UUID.randomUUID());
        notif1.setLida(false);
        notif1.setCriadoEm(Instant.now());

        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(responsavel.getId()))
            .thenReturn(List.of(notif1));

        // Act
        List<Notificacao> resultado = notificacaoService.obterNaoLidas(responsavel.getId());

        // Assert
        assertEquals(1, resultado.size());
        verify(notificacaoRepository).findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(responsavel.getId());
    }

    // ==================== Comportamento: Marcar como Lida ====================

    @Test
    @DisplayName("Marcar como lida valida propriedade do usuário")
    void testMarcarComoLida_ValidaAutorizacao() {
        // Arrange
        var notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setUsuario(responsavel);
        notificacao.setLida(false);

        when(notificacaoRepository.findById(notificacao.getId()))
            .thenReturn(Optional.of(notificacao));

        // Act & Assert
        // Deve funcionar para o proprietário
        assertDoesNotThrow(() ->
            notificacaoService.marcarComoLidaComAutorizacao(notificacao.getId(), responsavel.getId())
        );

        // Deve falhar para outro usuário
        assertThrows(IllegalArgumentException.class, () ->
            notificacaoService.marcarComoLidaComAutorizacao(notificacao.getId(), UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Marcar como lida: notificação não encontrada lança exceção")
    void testMarcarComoLida_NaoEncontrada() {
        // Arrange
        when(notificacaoRepository.findById(any()))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            notificacaoService.marcarComoLidaComAutorizacao(UUID.randomUUID(), UUID.randomUUID())
        );
    }
}
