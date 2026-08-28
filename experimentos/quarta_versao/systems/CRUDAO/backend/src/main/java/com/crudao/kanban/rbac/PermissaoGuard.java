package com.crudao.kanban.rbac;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guard reutilizável de autorização (RNF-003), usado por todos os controllers de escrita das
 * demais epics (03.x, 04.x, 05.x).
 *
 * <p>Uso via {@code @PreAuthorize}: {@code @PreAuthorize("@permissaoGuard.permitido(#projetoId,
 * 'papel:administrar')")} — o parâmetro do controller referenciado em {@code #projetoId} precisa
 * declarar o nome explicitamente (ex.: {@code @PathVariable("projetoId")}), já que o projeto não
 * compila com a flag {@code -parameters}. Uso imperativo em service: {@code
 * permissaoGuard.exigir(projetoId, "papel:administrar")} — lança {@link AccessDeniedException},
 * traduzida pelo Spring Security para {@code 403}.
 *
 * <p>Usuário com {@link Usuario#isAdminGlobal()} (bootstrap, ADR-007) bypassa toda checagem de
 * permissão/vínculo escopada por projeto — é o único capaz de administrar antes de qualquer
 * vínculo local existir.
 */
@Component("permissaoGuard")
public class PermissaoGuard {

    private final PermissaoService permissaoService;
    private final ProjetoRepository projetoRepository;

    public PermissaoGuard(PermissaoService permissaoService, ProjetoRepository projetoRepository) {
        this.permissaoService = permissaoService;
        this.projetoRepository = projetoRepository;
    }

    public boolean permitido(UUID projetoId, String permissaoChave) {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || !usuario.isAtivo()) {
            return false;
        }
        if (usuario.isAdminGlobal()) {
            return true;
        }
        return permissaoService.possui(usuario.getId(), projetoId, permissaoChave);
    }

    public void exigir(UUID projetoId, String permissaoChave) {
        if (!permitido(projetoId, permissaoChave)) {
            // Mensagem genérica — não expor a chave de permissão exigida na resposta ao cliente
            // (enumeração de permissões internas via mensagens de erro).
            throw new AccessDeniedException("Acesso negado");
        }
    }

    /**
     * {@code true} se o usuário autenticado tem qualquer vínculo com o projeto — usado pelos GETs
     * de leitura de `papeis-permissoes.md`, que exigem apenas vínculo ao projeto (não uma
     * permissão específica).
     */
    public boolean membro(UUID projetoId) {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || !usuario.isAtivo()) {
            return false;
        }
        if (usuario.isAdminGlobal()) {
            return true;
        }
        return permissaoService.possuiVinculo(usuario.getId(), projetoId);
    }

    /**
     * Guard reutilizável "projeto finalizado → somente leitura" (RN-015) — chamado por todo
     * endpoint de escrita de tarefas/workflow/raia (Epic 04+) antes de gravar. Bloqueia inclusive
     * o admin global — RN-015 não abre exceção para nenhum papel.
     */
    public void exigirProjetoAtivo(UUID projetoId) {
        Projeto projeto =
                projetoRepository
                        .findById(projetoId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado"));
        if (projeto.getStatus() == Projeto.Status.FINALIZADO) {
            throw new AccessDeniedException("Acesso negado");
        }
    }
}
