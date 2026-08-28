package com.crudao.kanban.rbac;

import com.crudao.kanban.domain.papel.PapelPermissao;
import com.crudao.kanban.domain.papel.PapelPermissaoRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Resolução das permissões efetivas de um usuário em um projeto (RF-013, RF-015, RF-016) — motor
 * central de autorização usado por {@link PermissaoGuard} e por todos os endpoints de escrita das
 * demais epics (RNF-003).
 *
 * <p>Uma permissão só é efetiva quando: o {@link Usuario} está {@code ativo} (revalidado aqui, não
 * apenas confiado do contexto de autenticação — cobre remoção/desativação sem revogação de
 * sessão); existe {@link UsuarioProjetoPapel} vinculando o usuário ao projeto via algum papel; e o
 * papel tem a permissão habilitada via toggle ({@link PapelPermissao#isHabilitada()}).
 */
@Service
public class PermissaoService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final PapelPermissaoRepository papelPermissaoRepository;

    public PermissaoService(
            UsuarioRepository usuarioRepository,
            UsuarioProjetoPapelRepository usuarioProjetoPapelRepository,
            PapelPermissaoRepository papelPermissaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
        this.papelPermissaoRepository = papelPermissaoRepository;
    }

    /** Conjunto de chaves de permissão habilitadas para o usuário no projeto (união dos papéis). */
    public Set<String> permissoesEfetivas(UUID usuarioId, UUID projetoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return Set.of();
        }

        List<UsuarioProjetoPapel> vinculos =
                usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId);
        if (vinculos.isEmpty()) {
            return Set.of();
        }

        List<UUID> papelIds = vinculos.stream().map(v -> v.getPapel().getId()).toList();

        Set<String> permissoes = new HashSet<>();
        for (PapelPermissao papelPermissao : papelPermissaoRepository.findByPapelIdIn(papelIds)) {
            if (papelPermissao.isHabilitada()) {
                permissoes.add(papelPermissao.getPermissao().getChave());
            }
        }
        return permissoes;
    }

    public boolean possui(UUID usuarioId, UUID projetoId, String permissaoChave) {
        return permissoesEfetivas(usuarioId, projetoId).contains(permissaoChave);
    }

    /** {@code true} se o usuário (ativo) tem qualquer vínculo (papel) com o projeto. */
    public boolean possuiVinculo(UUID usuarioId, UUID projetoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return false;
        }
        return !usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId).isEmpty();
    }
}
