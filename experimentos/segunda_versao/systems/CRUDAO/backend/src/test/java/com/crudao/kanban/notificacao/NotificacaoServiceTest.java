package com.crudao.kanban.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.notificacao.Notificacao;
import com.crudao.kanban.domain.notificacao.NotificacaoRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservador;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.usuario.Usuario;
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

/** Notificações internas — TASK-05.2 (RF-005). */
@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private TarefaObservadorRepository tarefaObservadorRepository;
    @Mock private NotificacaoPublisher notificacaoPublisher;

    private NotificacaoService service;

    @BeforeEach
    void setUp() {
        service = new NotificacaoService(notificacaoRepository, tarefaObservadorRepository, notificacaoPublisher);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    private Usuario usuario(UUID id) {
        Usuario u = new Usuario();
        u.setId(id);
        return u;
    }

    private Tarefa tarefaCom(UUID responsavelId, UUID criadoPorId) {
        Tarefa tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        tarefa.setTitulo("Tarefa X");
        if (responsavelId != null) {
            tarefa.setResponsavel(usuario(responsavelId));
        }
        if (criadoPorId != null) {
            tarefa.setCriadoPor(usuario(criadoPorId));
        }
        return tarefa;
    }

    @Test
    void notificarObservadores_responsavelCriadorEObservadores_criaUmaNotificacaoPorPessoaSemDuplicar() {
        UUID responsavelId = UUID.randomUUID();
        UUID criadorId = UUID.randomUUID();
        UUID observadorExplicitoId = UUID.randomUUID();
        Tarefa tarefa = tarefaCom(responsavelId, criadorId);
        when(tarefaObservadorRepository.findByTarefaId(tarefa.getId()))
                .thenReturn(
                        List.of(
                                new TarefaObservador(tarefa.getId(), observadorExplicitoId),
                                // duplicata do responsável — não deve gerar notificação repetida
                                new TarefaObservador(tarefa.getId(), responsavelId)));
        when(notificacaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.notificarObservadores(tarefa, NotificacaoService.TIPO_TRANSICAO_ETAPA, "mensagem");

        var captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository, times(3)).save(captor.capture());
        List<UUID> destinatarios = captor.getAllValues().stream().map(n -> n.getUsuario().getId()).toList();
        assertThat(destinatarios).containsExactlyInAnyOrder(responsavelId, criadorId, observadorExplicitoId);
        verify(notificacaoPublisher, times(3)).publicar(any());
    }

    @Test
    void notificarObservadores_semResponsavelNemCriador_naoLancaESemNotificacao() {
        Tarefa tarefa = tarefaCom(null, null);
        when(tarefaObservadorRepository.findByTarefaId(tarefa.getId())).thenReturn(List.of());

        service.notificarObservadores(tarefa, NotificacaoService.TIPO_IMPEDIMENTO_MARCADO, "mensagem");

        verify(notificacaoRepository, never()).save(any());
        verify(notificacaoPublisher, never()).publicar(any());
    }

    @Test
    void listar_retornaApenasNaoLidasDoUsuarioAutenticado() {
        UUID usuarioId = UUID.randomUUID();
        UsuarioAutenticadoHolder.set(usuario(usuarioId));
        Tarefa tarefa = tarefaCom(null, null);
        Notificacao notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setTarefa(tarefa);
        notificacao.setTipo(NotificacaoService.TIPO_TRANSICAO_ETAPA);
        notificacao.setMensagem("msg");
        notificacao.setLida(false);
        notificacao.setCriadoEm(java.time.OffsetDateTime.now());
        when(notificacaoRepository.findByUsuarioIdAndLidaFalseOrderByCriadoEmDesc(usuarioId))
                .thenReturn(List.of(notificacao));

        List<NotificacaoResponse> resultado = service.listar();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).id()).isEqualTo(notificacao.getId());
        assertThat(resultado.get(0).lida()).isFalse();
    }

    @Test
    void listar_semUsuarioAutenticado_lanca403() {
        assertThatThrownBy(() -> service.listar()).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void marcarComoLida_notificacaoDoUsuario_marcaLida() {
        UUID usuarioId = UUID.randomUUID();
        UsuarioAutenticadoHolder.set(usuario(usuarioId));
        Notificacao notificacao = new Notificacao();
        notificacao.setId(UUID.randomUUID());
        notificacao.setLida(false);
        when(notificacaoRepository.findByIdAndUsuarioId(notificacao.getId(), usuarioId))
                .thenReturn(Optional.of(notificacao));

        service.marcarComoLida(notificacao.getId());

        assertThat(notificacao.isLida()).isTrue();
        verify(notificacaoRepository).save(notificacao);
    }

    @Test
    void marcarComoLida_notificacaoDeOutroUsuarioOuInexistente_lanca404() {
        UUID usuarioId = UUID.randomUUID();
        UsuarioAutenticadoHolder.set(usuario(usuarioId));
        UUID id = UUID.randomUUID();
        when(notificacaoRepository.findByIdAndUsuarioId(id, usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarComoLida(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
