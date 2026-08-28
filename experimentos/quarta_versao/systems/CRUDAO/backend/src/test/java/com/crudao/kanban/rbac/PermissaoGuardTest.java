package com.crudao.kanban.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
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

/** Guard reutilizável de autorização (RNF-003) — TASK-02.2, TASK-03.1 (admin global, projeto ativo). */
@ExtendWith(MockitoExtension.class)
class PermissaoGuardTest {

    @Mock private PermissaoService permissaoService;
    @Mock private ProjetoRepository projetoRepository;

    private PermissaoGuard guard;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID projetoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        guard = new PermissaoGuard(permissaoService, projetoRepository);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void quandoNaoHaUsuarioAutenticadoNoContexto_negaPermissao() {
        UsuarioAutenticadoHolder.clear();

        assertThat(guard.permitido(projetoId, "papel:administrar")).isFalse();
    }

    @Test
    void quandoUsuarioAutenticadoPossuiPermissao_permite() {
        UsuarioAutenticadoHolder.set(usuario());
        when(permissaoService.possui(usuarioId, projetoId, "papel:administrar")).thenReturn(true);

        assertThat(guard.permitido(projetoId, "papel:administrar")).isTrue();
    }

    @Test
    void quandoUsuarioAutenticadoNaoPossuiPermissao_negaEExigirLancaAccessDenied() {
        UsuarioAutenticadoHolder.set(usuario());
        when(permissaoService.possui(usuarioId, projetoId, "papel:administrar")).thenReturn(false);

        assertThat(guard.permitido(projetoId, "papel:administrar")).isFalse();
        assertThatThrownBy(() -> guard.exigir(projetoId, "papel:administrar"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void quandoUsuarioInativoNegaMesmoSendoAdminGlobal() {
        Usuario usuario = usuarioAdminGlobal();
        usuario.setAtivo(false);
        UsuarioAutenticadoHolder.set(usuario);

        assertThat(guard.permitido(projetoId, "papel:administrar")).isFalse();
        assertThat(guard.membro(projetoId)).isFalse();
        verifyNoInteractions(permissaoService);
    }

    @Test
    void quandoExigirComPermissaoConcedida_naoLancaExcecao() {
        UsuarioAutenticadoHolder.set(usuario());
        when(permissaoService.possui(usuarioId, projetoId, "papel:administrar")).thenReturn(true);

        guard.exigir(projetoId, "papel:administrar");
    }

    @Test
    void quandoUsuarioAdminGlobal_permiteMesmoSemPermissaoEscopada() {
        UsuarioAutenticadoHolder.set(usuarioAdminGlobal());

        assertThat(guard.permitido(projetoId, "papel:administrar")).isTrue();
        assertThat(guard.membro(projetoId)).isTrue();
        verifyNoInteractions(permissaoService);
    }

    @Test
    void exigirProjetoAtivo_comProjetoFinalizado_lancaAccessDenied() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setStatus(Projeto.Status.FINALIZADO);
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> guard.exigirProjetoAtivo(projetoId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void exigirProjetoAtivo_comProjetoAtivo_naoLancaExcecao() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setStatus(Projeto.Status.ATIVO);
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));

        guard.exigirProjetoAtivo(projetoId);
    }

    @Test
    void exigirProjetoAtivo_comProjetoInexistente_lanca404() {
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.exigirProjetoAtivo(projetoId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setAtivo(true);
        return usuario;
    }

    private Usuario usuarioAdminGlobal() {
        Usuario usuario = usuario();
        usuario.setAdminGlobal(true);
        return usuario;
    }
}
