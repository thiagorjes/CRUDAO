package com.crudao.kanban.papel;

import com.crudao.kanban.auth.UsuarioAutenticadoHolder;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelPermissao;
import com.crudao.kanban.domain.papel.PapelPermissaoAuditoria;
import com.crudao.kanban.domain.papel.PapelPermissaoAuditoriaRepository;
import com.crudao.kanban.domain.papel.PapelPermissaoId;
import com.crudao.kanban.domain.papel.PapelPermissaoRepository;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.Permissao;
import com.crudao.kanban.domain.papel.PermissaoRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * CRUD de {@link Papel} escopado por projeto e toggles de {@link PapelPermissao} (RF-013, RF-015,
 * RF-016). {@code admin} (global, {@code protegido=true}) nunca é editável/excluível nem tem seus
 * toggles alterados — RN-006.
 */
@Service
public class PapelService {

    private static final String CHAVE_ADMIN = "admin";
    private static final String PERMISSAO_ADMINISTRAR = "papel:administrar";

    private final PapelRepository papelRepository;
    private final PapelPermissaoRepository papelPermissaoRepository;
    private final PermissaoRepository permissaoRepository;
    private final PapelPermissaoAuditoriaRepository auditoriaRepository;
    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    public PapelService(
            PapelRepository papelRepository,
            PapelPermissaoRepository papelPermissaoRepository,
            PermissaoRepository permissaoRepository,
            PapelPermissaoAuditoriaRepository auditoriaRepository,
            UsuarioProjetoPapelRepository usuarioProjetoPapelRepository,
            ProjetoRepository projetoRepository,
            PermissaoGuard permissaoGuard) {
        this.papelRepository = papelRepository;
        this.papelPermissaoRepository = papelPermissaoRepository;
        this.permissaoRepository = permissaoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
        this.projetoRepository = projetoRepository;
        this.permissaoGuard = permissaoGuard;
    }

    @Transactional(readOnly = true)
    public List<PapelResponse> listarPorProjeto(UUID projetoId) {
        return papelRepository.findByProjetoId(projetoId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PapelResponse criar(UUID projetoId, CriarPapelRequest request) {
        if (CHAVE_ADMIN.equalsIgnoreCase(request.chave())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "chave 'admin' é reservada");
        }

        Projeto projeto =
                projetoRepository
                        .findById(projetoId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado"));

        if (papelRepository.findByProjetoIdAndChave(projetoId, request.chave()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "chave já usada por outro papel do projeto");
        }

        Papel papel = new Papel();
        papel.setProjeto(projeto);
        papel.setChave(request.chave());
        papel.setNome(request.nome());
        papel.setProtegido(false);
        papel = papelRepository.save(papel);

        // Cria os toggles (desabilitados) para todo o catálogo — RF-016 exige uma entrada por
        // permissão para cada papel, mesmo antes de qualquer alteração.
        for (Permissao permissao : permissaoRepository.findAll()) {
            PapelPermissao papelPermissao = new PapelPermissao();
            papelPermissao.setPapel(papel);
            papelPermissao.setPermissao(permissao);
            papelPermissao.setHabilitada(false);
            papelPermissaoRepository.save(papelPermissao);
        }

        return toResponse(papel);
    }

    @Transactional
    public PapelResponse editar(UUID papelId, EditarPapelRequest request) {
        Papel papel = buscarPapel(papelId);
        exigirNaoProtegidoEAutorizado(papel);

        papel.setNome(request.nome());
        return toResponse(papelRepository.save(papel));
    }

    @Transactional
    public void excluir(UUID papelId) {
        Papel papel = buscarPapel(papelId);
        exigirNaoProtegidoEAutorizado(papel);

        if (usuarioProjetoPapelRepository.existsByPapelId(papelId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "papel possui usuários vinculados");
        }

        papelPermissaoRepository.deleteAll(papelPermissaoRepository.findByPapelId(papelId));
        papelRepository.delete(papel);
    }

    @Transactional
    public PapelResponse togglePermissao(UUID papelId, String permissaoChave, boolean habilitada) {
        Papel papel = buscarPapel(papelId);
        exigirNaoProtegidoEAutorizado(papel);

        Usuario autor = usuarioAutenticado();
        boolean autorPossuiEstePapel =
                usuarioProjetoPapelRepository
                        .findByUsuarioIdAndProjetoId(autor.getId(), papel.getProjeto().getId())
                        .stream()
                        .anyMatch(vinculo -> vinculo.getPapel().getId().equals(papelId));
        if (autorPossuiEstePapel) {
            // RN-017 — previne autoconcessão de privilégio: outro usuário com papel:administrar
            // precisa executar a alteração.
            throw new AccessDeniedException("Acesso negado");
        }

        Permissao permissao =
                permissaoRepository
                        .findByChave(permissaoChave)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "permissão não encontrada"));

        PapelPermissaoId id = new PapelPermissaoId(papelId, permissao.getId());
        PapelPermissao papelPermissao =
                papelPermissaoRepository
                        .findById(id)
                        .orElseGet(
                                () -> {
                                    PapelPermissao novo = new PapelPermissao();
                                    novo.setPapel(papel);
                                    novo.setPermissao(permissao);
                                    novo.setHabilitada(false);
                                    return novo;
                                });

        boolean valorAnterior = papelPermissao.isHabilitada();
        papelPermissao.setHabilitada(habilitada);
        papelPermissaoRepository.save(papelPermissao);

        PapelPermissaoAuditoria auditoria = new PapelPermissaoAuditoria();
        auditoria.setPapel(papel);
        auditoria.setPermissao(permissao);
        auditoria.setAutor(autor);
        auditoria.setValorAnterior(valorAnterior);
        auditoria.setValorNovo(habilitada);
        auditoria.setDataHora(OffsetDateTime.now());
        auditoriaRepository.save(auditoria);

        return toResponse(papel);
    }

    private Papel buscarPapel(UUID papelId) {
        return papelRepository
                .findById(papelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "papel não encontrado"));
    }

    /** RN-006 (protegido bloqueia sempre) + RNF-003 (permissão validada no backend). */
    private void exigirNaoProtegidoEAutorizado(Papel papel) {
        if (papel.isProtegido()) {
            throw new AccessDeniedException("Acesso negado");
        }
        permissaoGuard.exigir(papel.getProjeto().getId(), PERMISSAO_ADMINISTRAR);
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null) {
            throw new AccessDeniedException("Acesso negado");
        }
        return usuario;
    }

    private PapelResponse toResponse(Papel papel) {
        List<PapelResponse.PermissaoToggleResponse> permissoes =
                papelPermissaoRepository.findByPapelId(papel.getId()).stream()
                        .map(pp -> new PapelResponse.PermissaoToggleResponse(pp.getPermissao().getChave(), pp.isHabilitada()))
                        .toList();
        return new PapelResponse(papel.getId(), papel.getChave(), papel.getNome(), papel.isProtegido(), permissoes);
    }
}
