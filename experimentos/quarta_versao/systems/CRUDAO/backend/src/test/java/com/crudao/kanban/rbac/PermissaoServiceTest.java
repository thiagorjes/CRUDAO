package com.crudao.kanban.rbac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelPermissao;
import com.crudao.kanban.domain.papel.PapelPermissaoRepository;
import com.crudao.kanban.domain.papel.Permissao;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Resolução de permissões efetivas (RF-013, RF-015, RF-016) — TASK-02.2. */
@ExtendWith(MockitoExtension.class)
class PermissaoServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    @Mock private PapelPermissaoRepository papelPermissaoRepository;

    private PermissaoService service;

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID projetoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new PermissaoService(
                        usuarioRepository, usuarioProjetoPapelRepository, papelPermissaoRepository);
    }

    @Test
    void quandoUsuarioInativo_naoPossuiNenhumaPermissaoIndependenteDoVinculo() {
        Usuario usuario = usuarioAtivo(false);
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        boolean resultado = service.possui(usuarioId, projetoId, "papel:administrar");

        assertThat(resultado).isFalse();
        assertThat(service.permissoesEfetivas(usuarioId, projetoId)).isEmpty();
    }

    @Test
    void quandoUsuarioNaoExiste_naoPossuiPermissao() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThat(service.possui(usuarioId, projetoId, "papel:administrar")).isFalse();
    }

    @Test
    void quandoUsuarioSemVinculoAoProjeto_naoPossuiPermissao() {
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioAtivo(true)));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId))
                .thenReturn(List.of());

        assertThat(service.possui(usuarioId, projetoId, "papel:administrar")).isFalse();
    }

    @Test
    void quandoPapelTemPermissaoHabilitada_usuarioPossuiPermissao() {
        Papel papel = papel("dev");
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioAtivo(true)));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId))
                .thenReturn(List.of(vinculo(papel)));
        when(papelPermissaoRepository.findByPapelIdIn(List.of(papel.getId())))
                .thenReturn(List.of(papelPermissao(papel, "tarefa:gerenciar", true)));

        assertThat(service.possui(usuarioId, projetoId, "tarefa:gerenciar")).isTrue();
    }

    @Test
    void quandoToggleDesabilitado_bloqueiaAcaoMesmoComPapelQueNormalmentePermite() {
        Papel papel = papel("dev");
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioAtivo(true)));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId))
                .thenReturn(List.of(vinculo(papel)));
        when(papelPermissaoRepository.findByPapelIdIn(List.of(papel.getId())))
                .thenReturn(List.of(papelPermissao(papel, "tarefa:gerenciar", false)));

        assertThat(service.possui(usuarioId, projetoId, "tarefa:gerenciar")).isFalse();
    }

    @Test
    void quandoUsuarioTemMultiplosPapeis_permissoesEfetivasSaoUniaoDosPapeis() {
        Papel papelDev = papel("dev");
        Papel papelGestor = papel("gestor");
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuarioAtivo(true)));
        when(usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId))
                .thenReturn(List.of(vinculo(papelDev), vinculo(papelGestor)));
        when(papelPermissaoRepository.findByPapelIdIn(List.of(papelDev.getId(), papelGestor.getId())))
                .thenReturn(
                        List.of(
                                papelPermissao(papelDev, "tarefa:gerenciar", true),
                                papelPermissao(papelGestor, "papel:administrar", true)));

        Set<String> efetivas = service.permissoesEfetivas(usuarioId, projetoId);

        assertThat(efetivas).containsExactlyInAnyOrder("tarefa:gerenciar", "papel:administrar");
    }

    private Usuario usuarioAtivo(boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setAtivo(ativo);
        return usuario;
    }

    private static Papel papel(String chave) {
        Papel papel = new Papel();
        papel.setId(UUID.randomUUID());
        papel.setChave(chave);
        papel.setNome(chave);
        return papel;
    }

    private UsuarioProjetoPapel vinculo(Papel papel) {
        UsuarioProjetoPapel vinculo = new UsuarioProjetoPapel();
        vinculo.setPapel(papel);
        return vinculo;
    }

    private static PapelPermissao papelPermissao(Papel papel, String chave, boolean habilitada) {
        Permissao permissao = new Permissao();
        permissao.setId(UUID.randomUUID());
        permissao.setChave(chave);
        permissao.setDescricao(chave);

        PapelPermissao papelPermissao = new PapelPermissao();
        papelPermissao.setPapel(papel);
        papelPermissao.setPermissao(permissao);
        papelPermissao.setHabilitada(habilitada);
        return papelPermissao;
    }
}
