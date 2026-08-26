package com.crudao.kanban.papel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/**
 * Associação usuário↔projeto↔papel (RF-015) — TASK-02.3. Cobre em especial RN-006: `admin`
 * (global, protegido) nunca pode ser concedido por este CRUD (achado do code review — escalação
 * de privilégio).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioProjetoPapelServiceTest {

    @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProjetoRepository projetoRepository;
    @Mock private PapelRepository papelRepository;

    private UsuarioProjetoPapelService service;

    private final UUID projetoId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new UsuarioProjetoPapelService(
                        usuarioProjetoPapelRepository, usuarioRepository, projetoRepository, papelRepository);
    }

    @Test
    void associar_comPapelAdminGlobalProtegido_retorna422SemSalvar() {
        Papel admin = new Papel();
        admin.setId(UUID.randomUUID());
        admin.setChave("admin");
        admin.setProtegido(true);
        admin.setProjeto(null);

        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(new Projeto()));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(new Usuario()));
        when(papelRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(
                        () -> service.associar(projetoId, new AssociarUsuarioRequest(usuarioId, admin.getId())))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("422");
        verify(usuarioProjetoPapelRepository, never()).save(any());
    }

    @Test
    void associar_comPapelDoProjeto_associaComSucesso() {
        Projeto projeto = new Projeto();
        projeto.setId(projetoId);

        Papel papelDoProjeto = new Papel();
        papelDoProjeto.setId(UUID.randomUUID());
        papelDoProjeto.setChave("dev");
        papelDoProjeto.setProtegido(false);
        papelDoProjeto.setProjeto(projeto);

        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);

        when(projetoRepository.findById(projetoId)).thenReturn(Optional.of(projeto));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(papelRepository.findById(papelDoProjeto.getId())).thenReturn(Optional.of(papelDoProjeto));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoIdAndPapelId(
                        usuarioId, projetoId, papelDoProjeto.getId()))
                .thenReturn(Optional.empty());

        service.associar(projetoId, new AssociarUsuarioRequest(usuarioId, papelDoProjeto.getId()));

        verify(usuarioProjetoPapelRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void buscar_comMenosDeTresCaracteres_retornaVazioSemConsultarRepositorio() {
        assertThat(service.buscar(projetoId, "ab")).isEmpty();
        assertThat(service.buscar(projetoId, null)).isEmpty();
        verify(usuarioRepository, never())
                .buscarNaoAssociados(any(), any(), ArgumentMatchers.any());
    }

    @Test
    void buscar_comTermoValido_retornaResumoSemDadosSensiveis() {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Ana Silva");
        usuario.setEmail("ana@exemplo.com");
        usuario.setKeycloakSub("sub-123");
        usuario.setAdminGlobal(true);

        when(usuarioRepository.buscarNaoAssociados(
                        org.mockito.Mockito.eq(projetoId), org.mockito.Mockito.eq("ana"), ArgumentMatchers.any()))
                .thenReturn(List.of(usuario));

        List<UsuarioResumoResponse> resultado = service.buscar(projetoId, "ana");

        assertThat(resultado).containsExactly(new UsuarioResumoResponse(usuarioId, "Ana Silva", "ana@exemplo.com"));
    }
}
