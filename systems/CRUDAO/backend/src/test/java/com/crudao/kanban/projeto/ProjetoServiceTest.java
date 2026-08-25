package com.crudao.kanban.projeto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
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

/** CRUD de projeto incl. finalizar/reabrir — TASK-03.1 (RF-008, RN-015, ADR-007). */
@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock private ProjetoRepository projetoRepository;
    @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private ProjetoService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProjetoService(projetoRepository, usuarioProjetoPapelRepository, permissaoGuard);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void criar_semAdminGlobal_lanca403() {
        UsuarioAutenticadoHolder.set(usuario(false));

        assertThatThrownBy(() -> service.criar(new CriarProjetoRequest("Projeto X", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void criar_comAdminGlobalENomeVazio_lanca422() {
        UsuarioAutenticadoHolder.set(usuario(true));

        assertThatThrownBy(() -> service.criar(new CriarProjetoRequest(" ", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void criar_comAdminGlobal_criaProjetoAtivo() {
        UsuarioAutenticadoHolder.set(usuario(true));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoResponse resposta = service.criar(new CriarProjetoRequest("Projeto X", "desc"));

        assertThat(resposta.nome()).isEqualTo("Projeto X");
        assertThat(resposta.status()).isEqualTo(Projeto.Status.ATIVO);
    }

    @Test
    void editar_semPermissao_lanca403SemAlterarProjeto() {
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "projeto:administrar");

        assertThatThrownBy(() -> service.editar(projetoId, new EditarProjetoRequest("Novo nome", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void editar_comProjetoFinalizado_lanca403RN015() {
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigirProjetoAtivo(projetoId);

        assertThatThrownBy(() -> service.editar(projetoId, new EditarProjetoRequest("Novo nome", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void editar_autorizadoEAtivo_atualizaNomeEDescricao() {
        Projeto projeto = projetoAtivo();
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoResponse resposta = service.editar(projetoId, new EditarProjetoRequest("Novo nome", "nova desc"));

        assertThat(resposta.nome()).isEqualTo("Novo nome");
        assertThat(resposta.descricao()).isEqualTo("nova desc");
        verify(permissaoGuard).exigir(projetoId, "projeto:administrar");
        verify(permissaoGuard).exigirProjetoAtivo(projetoId);
    }

    @Test
    void finalizar_autorizado_marcaFinalizadoComTimestamp() {
        Projeto projeto = projetoAtivo();
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoResponse resposta = service.finalizar(projetoId);

        assertThat(resposta.status()).isEqualTo(Projeto.Status.FINALIZADO);
        assertThat(resposta.finalizadoEm()).isNotNull();
    }

    @Test
    void finalizar_semPermissao_lanca403() {
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(permissaoGuard)
                .exigir(projetoId, "projeto:administrar");

        assertThatThrownBy(() -> service.finalizar(projetoId)).isInstanceOf(AccessDeniedException.class);
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void reabrir_autorizado_restauraCapacidadeDeEdicao() {
        Projeto projeto = projetoAtivo();
        projeto.setStatus(Projeto.Status.FINALIZADO);
        projeto.setFinalizadoEm(java.time.OffsetDateTime.now());
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(projetoRepository.save(any(Projeto.class))).thenAnswer(inv -> inv.getArgument(0));

        ProjetoResponse resposta = service.reabrir(projetoId);

        assertThat(resposta.status()).isEqualTo(Projeto.Status.ATIVO);
        assertThat(resposta.finalizadoEm()).isNull();
    }

    @Test
    void listarVisiveis_comAdminGlobal_retornaTodosOsProjetos() {
        UsuarioAutenticadoHolder.set(usuario(true));
        when(projetoRepository.findAll()).thenReturn(List.of(projetoAtivo()));

        List<ProjetoResponse> resposta = service.listarVisiveis();

        assertThat(resposta).hasSize(1);
        verify(usuarioProjetoPapelRepository, never()).findByUsuarioId(any());
    }

    @Test
    void listarVisiveis_semAdminGlobal_retornaApenasVinculados() {
        UsuarioAutenticadoHolder.set(usuario(false));
        UsuarioProjetoPapel vinculo = new UsuarioProjetoPapel();
        vinculo.setProjeto(projetoAtivo());
        when(usuarioProjetoPapelRepository.findByUsuarioId(usuarioId)).thenReturn(List.of(vinculo));

        List<ProjetoResponse> resposta = service.listarVisiveis();

        assertThat(resposta).hasSize(1);
        verify(projetoRepository, never()).findAll();
    }

    private Usuario usuario(boolean adminGlobal) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setAtivo(true);
        usuario.setAdminGlobal(adminGlobal);
        return usuario;
    }

    private Projeto projetoAtivo() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        projeto.setNome("Projeto X");
        projeto.setStatus(Projeto.Status.ATIVO);
        return projeto;
    }
}
