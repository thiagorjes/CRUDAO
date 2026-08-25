package com.crudao.kanban.tarefa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.tarefa.TarefaObservadorRepository;
import com.crudao.kanban.domain.tarefa.TarefaRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/** CRUD de TarefaObservador — TASK-05.2 (RF-005). */
@ExtendWith(MockitoExtension.class)
class TarefaObservadorServiceTest {

    private static final String PERMISSAO_GERENCIAR = "tarefa:gerenciar";

    @Mock private TarefaObservadorRepository tarefaObservadorRepository;
    @Mock private TarefaRepository tarefaRepository;
    @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private TarefaObservadorService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID autorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new TarefaObservadorService(
                        tarefaObservadorRepository, tarefaRepository, usuarioProjetoPapelRepository, permissaoGuard);
        Usuario autor = new Usuario();
        autor.setId(autorId);
        UsuarioAutenticadoHolder.set(autor);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    private Tarefa tarefa() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        Tarefa tarefa = new Tarefa();
        tarefa.setId(UUID.randomUUID());
        tarefa.setProjeto(projeto);
        return tarefa;
    }

    @Test
    void adicionar_autoObservacao_naoExigePermissaoDeGerenciar() {
        Tarefa tarefa = tarefa();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(autorId, projetoId))
                .thenReturn(List.of(new UsuarioProjetoPapel()));

        service.adicionar(tarefa.getId(), autorId);

        verify(tarefaObservadorRepository).save(any());
    }

    @Test
    void adicionar_outroUsuarioSemPermissaoDeGerenciar_lanca403() {
        Tarefa tarefa = tarefa();
        UUID outroUsuarioId = UUID.randomUUID();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(false);

        assertThatThrownBy(() -> service.adicionar(tarefa.getId(), outroUsuarioId))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaObservadorRepository, never()).save(any());
    }

    @Test
    void adicionar_outroUsuarioComPermissaoDeGerenciarEVinculado_adiciona() {
        Tarefa tarefa = tarefa();
        UUID outroUsuarioId = UUID.randomUUID();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(true);
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(outroUsuarioId, projetoId))
                .thenReturn(List.of(new UsuarioProjetoPapel()));

        service.adicionar(tarefa.getId(), outroUsuarioId);

        verify(tarefaObservadorRepository).save(any());
    }

    @Test
    void adicionar_usuarioNaoVinculadoAoProjeto_lanca422() {
        Tarefa tarefa = tarefa();
        UUID outroUsuarioId = UUID.randomUUID();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(true);
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(outroUsuarioId, projetoId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.adicionar(tarefa.getId(), outroUsuarioId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
    }

    @Test
    void adicionar_semVinculoAoProjeto_lanca403() {
        Tarefa tarefa = tarefa();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(false);

        assertThatThrownBy(() -> service.adicionar(tarefa.getId(), autorId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void remover_autoRemocao_naoExigePermissaoDeGerenciar() {
        Tarefa tarefa = tarefa();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);

        service.remover(tarefa.getId(), autorId);

        verify(tarefaObservadorRepository).deleteByTarefaIdAndUsuarioId(tarefa.getId(), autorId);
    }

    @Test
    void remover_outroUsuarioSemPermissao_lanca403() {
        Tarefa tarefa = tarefa();
        UUID outroUsuarioId = UUID.randomUUID();
        when(tarefaRepository.findById(tarefa.getId())).thenReturn(Optional.of(tarefa));
        when(permissaoGuard.membro(projetoId)).thenReturn(true);
        when(permissaoGuard.permitido(projetoId, PERMISSAO_GERENCIAR)).thenReturn(false);

        assertThatThrownBy(() -> service.remover(tarefa.getId(), outroUsuarioId))
                .isInstanceOf(AccessDeniedException.class);
        verify(tarefaObservadorRepository, never()).deleteByTarefaIdAndUsuarioId(any(), any());
    }

    @Test
    void listar_tarefaInexistente_lanca404() {
        UUID id = UUID.randomUUID();
        when(tarefaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
