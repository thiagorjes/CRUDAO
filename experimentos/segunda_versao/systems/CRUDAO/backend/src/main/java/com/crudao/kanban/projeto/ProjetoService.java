package com.crudao.kanban.projeto;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD de {@link Projeto} incl. finalizar/reabrir (RF-008, RN-015).
 *
 * <p>{@code criar} exige {@link Usuario#isAdminGlobal()} (ADR-007) — não há projeto ainda para
 * escopar {@code projeto:administrar}. Os demais endpoints delegam a {@link PermissaoGuard}, já
 * escopado pelo {@code id} do projeto existente.
 */
@Service
public class ProjetoService {

    private static final String PERMISSAO_ADMINISTRAR = "projeto:administrar";

    private final ProjetoRepository projetoRepository;
    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final PermissaoGuard permissaoGuard;

    public ProjetoService(
            ProjetoRepository projetoRepository,
            UsuarioProjetoPapelRepository usuarioProjetoPapelRepository,
            PermissaoGuard permissaoGuard) {
        this.projetoRepository = projetoRepository;
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
        this.permissaoGuard = permissaoGuard;
    }

    /** Projetos visíveis ao usuário autenticado — todos para admin global, senão só os vinculados. */
    public List<ProjetoResponse> listarVisiveis() {
        Usuario usuario = usuarioAutenticado();
        if (usuario.isAdminGlobal()) {
            return projetoRepository.findAll().stream().map(this::toResponse).toList();
        }
        return usuarioProjetoPapelRepository.findByUsuarioId(usuario.getId()).stream()
                .map(UsuarioProjetoPapel::getProjeto)
                .distinct()
                .sorted(Comparator.comparing(Projeto::getNome))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProjetoResponse criar(CriarProjetoRequest request) {
        Usuario usuario = usuarioAutenticado();
        if (!usuario.isAdminGlobal()) {
            // Bootstrap (ADR-007) — sem projeto existente não há como escopar projeto:administrar.
            throw new AccessDeniedException("Acesso negado");
        }
        exigirNomeValido(request.nome());

        Projeto projeto = new Projeto();
        projeto.setNome(request.nome());
        projeto.setDescricao(request.descricao());
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setCriadoPor(usuario);
        projeto.setCriadoEm(OffsetDateTime.now());
        return toResponse(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse editar(UUID id, EditarProjetoRequest request) {
        permissaoGuard.exigir(id, PERMISSAO_ADMINISTRAR);
        permissaoGuard.exigirProjetoAtivo(id);
        exigirNomeValido(request.nome());

        Projeto projeto = buscar(id);
        projeto.setNome(request.nome());
        projeto.setDescricao(request.descricao());
        return toResponse(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse finalizar(UUID id) {
        permissaoGuard.exigir(id, PERMISSAO_ADMINISTRAR);

        Projeto projeto = buscar(id);
        projeto.setStatus(Projeto.Status.FINALIZADO);
        projeto.setFinalizadoEm(OffsetDateTime.now());
        return toResponse(projetoRepository.save(projeto));
    }

    @Transactional
    public ProjetoResponse reabrir(UUID id) {
        permissaoGuard.exigir(id, PERMISSAO_ADMINISTRAR);

        Projeto projeto = buscar(id);
        projeto.setStatus(Projeto.Status.ATIVO);
        projeto.setFinalizadoEm(null);
        return toResponse(projetoRepository.save(projeto));
    }

    private void exigirNomeValido(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "nome é obrigatório");
        }
    }

    private Projeto buscar(UUID id) {
        return projetoRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado"));
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null) {
            throw new AccessDeniedException("Acesso negado");
        }
        return usuario;
    }

    private ProjetoResponse toResponse(Projeto projeto) {
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getNome(),
                projeto.getDescricao(),
                projeto.getStatus(),
                projeto.getFinalizadoEm());
    }
}
