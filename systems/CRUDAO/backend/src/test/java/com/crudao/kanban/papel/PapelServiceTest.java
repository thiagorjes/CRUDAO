package com.crudao.kanban.papel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelPermissao;
import com.crudao.kanban.domain.papel.PapelPermissaoAuditoriaRepository;
import com.crudao.kanban.domain.papel.PapelPermissaoRepository;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.Permissao;
import com.crudao.kanban.domain.papel.PermissaoRepository;
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

/** CRUD de papéis e toggles de permissão — TASK-02.3 (RN-006, RN-017). */
@ExtendWith(MockitoExtension.class)
class PapelServiceTest {

    @Mock private PapelRepository papelRepository;
    @Mock private PapelPermissaoRepository papelPermissaoRepository;
    @Mock private PermissaoRepository permissaoRepository;
    @Mock private PapelPermissaoAuditoriaRepository auditoriaRepository;
    @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private PermissaoGuard permissaoGuard;

    private PapelService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID autorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new PapelService(
                        papelRepository,
                        papelPermissaoRepository,
                        permissaoRepository,
                        auditoriaRepository,
                        usuarioProjetoPapelRepository,
                        projetoRepository,
                        permissaoGuard);

        Usuario autor = new Usuario();
        autor.setId(autorId);
        UsuarioAutenticadoHolder.set(autor);
    }

    @AfterEach
    void tearDown() {
        UsuarioAutenticadoHolder.clear();
    }

    @Test
    void criar_comChaveAdmin_retorna422() {
        assertThatThrownBy(() -> service.criar(projetoId, new CriarPapelRequest("admin", "Admin")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(papelRepository, never()).save(any());
    }

    @Test
    void criar_comChaveJaUsadaNoProjeto_retorna409() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(papelRepository.findByProjetoIdAndChave(projetoId, "dev-custom"))
                .thenReturn(Optional.of(papelDeProjeto()));

        assertThatThrownBy(() -> service.criar(projetoId, new CriarPapelRequest("dev-custom", "Dev Custom")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(papelRepository, never()).save(any());
    }

    @Test
    void criar_comChaveValida_criaPapelComTogglesDesabilitados() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);
        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(papelRepository.findByProjetoIdAndChave(projetoId, "dev-custom")).thenReturn(Optional.empty());
        when(papelRepository.save(any(Papel.class)))
                .thenAnswer(
                        invocation -> {
                            Papel papel = invocation.getArgument(0);
                            papel.setId(UUID.randomUUID());
                            return papel;
                        });
        when(permissaoRepository.findAll()).thenReturn(List.of(permissao("tarefa:gerenciar")));
        when(papelPermissaoRepository.findByPapelId(any())).thenReturn(List.of());

        PapelResponse resposta = service.criar(projetoId, new CriarPapelRequest("dev-custom", "Dev Custom"));

        assertThat(resposta.chave()).isEqualTo("dev-custom");
        assertThat(resposta.protegido()).isFalse();
        verify(papelPermissaoRepository, times(1)).save(any(PapelPermissao.class));
    }

    @Test
    void editar_papelProtegido_lanca403SemChamarGuard() {
        Papel admin = papelProtegido();
        when(papelRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.editar(admin.getId(), new EditarPapelRequest("Outro nome")))
                .isInstanceOf(AccessDeniedException.class);
        verify(permissaoGuard, never()).exigir(any(), any());
    }

    @Test
    void excluir_papelComUsuariosVinculados_retorna409() {
        Papel papel = papelDeProjeto();
        when(papelRepository.findById(papel.getId())).thenReturn(Optional.of(papel));
        lenient().when(usuarioProjetoPapelRepository.existsByPapelId(papel.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(papel.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(papelRepository, never()).delete(any());
    }

    @Test
    void togglePermissao_quandoAutorPossuiOPapelAlvo_retorna403RN017() {
        Papel papel = papelDeProjeto();
        when(papelRepository.findById(papel.getId())).thenReturn(Optional.of(papel));
        UsuarioProjetoPapel vinculoDoAutor = new UsuarioProjetoPapel();
        vinculoDoAutor.setPapel(papel);
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(autorId, projetoId))
                .thenReturn(List.of(vinculoDoAutor));

        assertThatThrownBy(() -> service.togglePermissao(papel.getId(), "tarefa:gerenciar", true))
                .isInstanceOf(AccessDeniedException.class);
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void togglePermissao_comAutorSemOPapelAlvo_alteraERegistraAuditoria() {
        Papel papel = papelDeProjeto();
        Permissao permissao = permissao("tarefa:gerenciar");
        when(papelRepository.findById(papel.getId())).thenReturn(Optional.of(papel));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(autorId, projetoId))
                .thenReturn(List.of());
        when(permissaoRepository.findByChave("tarefa:gerenciar")).thenReturn(Optional.of(permissao));
        when(papelPermissaoRepository.findById(any())).thenReturn(Optional.empty());
        when(papelPermissaoRepository.findByPapelId(papel.getId())).thenReturn(List.of());

        service.togglePermissao(papel.getId(), "tarefa:gerenciar", true);

        verify(permissaoGuard, times(1)).exigir(projetoId, "papel:administrar");
        verify(papelPermissaoRepository, times(1)).save(any(PapelPermissao.class));
        verify(auditoriaRepository, times(1)).save(any());
    }

    private static Papel papelProtegido() {
        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setChave("admin");
        papel.setNome("Administrador");
        papel.setProtegido(true);
        return papel;
    }

    private Papel papelDeProjeto() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);

        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setChave("dev-custom");
        papel.setNome("Dev Custom");
        papel.setProtegido(false);
        papel.setProjeto(projeto);
        return papel;
    }

    private static Permissao permissao(String chave) {
        Permissao permissao = new Permissao();
        permissao.setId(UUID.randomUUID());
        permissao.setChave(chave);
        permissao.setDescricao(chave);
        return permissao;
    }
}
