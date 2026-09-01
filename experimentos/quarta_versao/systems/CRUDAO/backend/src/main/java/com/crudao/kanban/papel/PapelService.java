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
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import com.crudao.kanban.papel.dto.AssociarUsuarioRequest;
import com.crudao.kanban.papel.dto.AtualizarPapelRequest;
import com.crudao.kanban.papel.dto.CriarPapelRequest;
import com.crudao.kanban.papel.dto.PapelResponse;
import com.crudao.kanban.papel.dto.TogglePermissaoRequest;
import com.crudao.kanban.papel.dto.UsuarioBuscaResponse;
import com.crudao.kanban.projeto.dto.UsuarioProjetoResponse;
import com.crudao.kanban.rbac.PermissaoGuard;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * TL-09/TL-10 — Papéis, Permissões e Usuários do Projeto.
 * Contrato: docs/techspec/kanban-tarefas/contracts/papeis-permissoes.md (RF-013, RF-015, RF-016).
 */
@Service
public class PapelService {

    private final PapelRepository papelRepository;
    private final PermissaoRepository permissaoRepository;
    private final PapelPermissaoRepository papelPermissaoRepository;
    private final PapelPermissaoAuditoriaRepository auditoriaRepository;
    private final UsuarioProjetoPapelRepository vinculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoRepository projetoRepository;
    private final PermissaoGuard permissaoGuard;

    public PapelService(
            PapelRepository papelRepository,
            PermissaoRepository permissaoRepository,
            PapelPermissaoRepository papelPermissaoRepository,
            PapelPermissaoAuditoriaRepository auditoriaRepository,
            UsuarioProjetoPapelRepository vinculoRepository,
            UsuarioRepository usuarioRepository,
            ProjetoRepository projetoRepository,
            PermissaoGuard permissaoGuard) {
        this.papelRepository = papelRepository;
        this.permissaoRepository = permissaoRepository;
        this.papelPermissaoRepository = papelPermissaoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.vinculoRepository = vinculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.projetoRepository = projetoRepository;
        this.permissaoGuard = permissaoGuard;
    }

    // ------------------------------------------------------------------
    // Papéis
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PapelResponse> listarPapeis(UUID projetoId) {
        exigirMembro(projetoId);
        return papelRepository.findByProjetoId(projetoId).stream().map(this::paraResponse).toList();
    }

    @Transactional
    public PapelResponse criarPapel(UUID projetoId, CriarPapelRequest request) {
        permissaoGuard.exigir(projetoId, "papel:administrar");
        permissaoGuard.exigirProjetoAtivo(projetoId);

        String chave = request.getChave().trim();
        if ("admin".equalsIgnoreCase(chave)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "chave 'admin' é reservada");
        }
        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado"));
        if (papelRepository.findByProjetoIdAndChave(projetoId, chave).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "já existe um papel com essa chave neste projeto");
        }

        Papel papel = new Papel();
        papel.setProjeto(projeto);
        papel.setChave(chave);
        papel.setNome(request.getNome().trim());
        papel.setProtegido(false);
        papel = papelRepository.save(papel);

        // Semeia um toggle (desabilitado) para cada permissão do catálogo — mesma convenção de
        // ProjetoService.criar, para que PUT /papeis/{id}/permissoes/{chave} sempre encontre a linha.
        for (Permissao permissao : permissaoRepository.findAll()) {
            papelPermissaoRepository.save(new PapelPermissao(papel, permissao, false));
        }

        return paraResponse(papel);
    }

    @Transactional
    public PapelResponse editarPapel(UUID papelId, AtualizarPapelRequest request) {
        Papel papel = obterPapel(papelId);
        exigirNaoProtegido(papel);
        permissaoGuard.exigir(projetoIdDe(papel), "papel:administrar");
        permissaoGuard.exigirProjetoAtivo(projetoIdDe(papel));

        papel.setNome(request.getNome().trim());
        return paraResponse(papelRepository.save(papel));
    }

    @Transactional
    public void excluirPapel(UUID papelId) {
        Papel papel = obterPapel(papelId);
        exigirNaoProtegido(papel);
        permissaoGuard.exigir(projetoIdDe(papel), "papel:administrar");
        permissaoGuard.exigirProjetoAtivo(projetoIdDe(papel));

        if (vinculoRepository.existsByPapelId(papelId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "há usuários vinculados a este papel");
        }
        papelPermissaoRepository.deleteAll(papelPermissaoRepository.findByPapelId(papelId));
        papelRepository.delete(papel);
    }

    @Transactional
    public PapelResponse togglePermissao(UUID papelId, String permissaoChave, TogglePermissaoRequest request) {
        Papel papel = obterPapel(papelId);
        exigirNaoProtegido(papel);
        UUID projetoId = projetoIdDe(papel);
        permissaoGuard.exigir(projetoId, "papel:administrar");
        permissaoGuard.exigirProjetoAtivo(projetoId);

        Usuario autor = usuarioAutenticado();
        // RN-017: autor não pode alterar PapelPermissao de um papel que ele próprio possui no
        // projeto — previne autoconcessão de privilégio. adminGlobal está isento (bootstrap).
        boolean autorPossuiEstePapel =
                vinculoRepository.findByUsuarioIdAndProjetoId(autor.getId(), projetoId).stream()
                        .anyMatch(v -> v.getPapel().getId().equals(papelId));
        if (!autor.isAdminGlobal() && autorPossuiEstePapel) {
            throw new AccessDeniedException("Não é permitido alterar permissões do próprio papel (RN-017)");
        }

        Permissao permissao = permissaoRepository.findByChave(permissaoChave)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "permissão não encontrada"));

        PapelPermissaoId id = new PapelPermissaoId(papelId, permissao.getId());
        PapelPermissao vinculo = papelPermissaoRepository.findById(id)
                .orElseGet(() -> new PapelPermissao(papel, permissao, false));

        boolean anterior = vinculo.isHabilitada();
        vinculo.setHabilitada(request.getHabilitada());
        papelPermissaoRepository.save(vinculo);

        PapelPermissaoAuditoria auditoria = new PapelPermissaoAuditoria();
        auditoria.setPapel(papel);
        auditoria.setPermissao(permissao);
        auditoria.setAutor(autor);
        auditoria.setValorAnterior(anterior);
        auditoria.setValorNovo(request.getHabilitada());
        auditoriaRepository.save(auditoria);

        return paraResponse(papel);
    }

    // ------------------------------------------------------------------
    // Usuários do projeto (associação)
    // ------------------------------------------------------------------

    @Transactional
    public void associarUsuario(UUID projetoId, AssociarUsuarioRequest request) {
        permissaoGuard.exigir(projetoId, "papel:administrar");
        permissaoGuard.exigirProjetoAtivo(projetoId);

        Projeto projeto = projetoRepository.findById(projetoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado"));
        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado"));
        Papel papel = obterPapel(request.getPapelId());

        // Papel admin (global, protegido) só pode ser associado pelo adminGlobal (achado do
        // Comitê: "administrador local" não pode conceder o papel admin a ninguém).
        if (papel.isProtegido() && !usuarioAutenticado().isAdminGlobal()) {
            throw new AccessDeniedException("Apenas o administrador global pode associar o papel admin");
        }
        if (!papel.isProtegido() && (papel.getProjeto() == null || !papel.getProjeto().getId().equals(projetoId))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "papel não pertence a este projeto");
        }

        if (vinculoRepository.findByUsuarioIdAndProjetoIdAndPapelId(usuario.getId(), projetoId, papel.getId())
                .isPresent()) {
            return; // idempotente
        }
        vinculoRepository.save(new UsuarioProjetoPapel(usuario, projeto, papel, OffsetDateTime.now()));
    }

    @Transactional
    public void removerUsuario(UUID projetoId, UUID usuarioId) {
        permissaoGuard.exigir(projetoId, "papel:administrar");
        permissaoGuard.exigirProjetoAtivo(projetoId);

        List<UsuarioProjetoPapel> vinculos = vinculoRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId);
        vinculoRepository.deleteAll(vinculos);
    }

    @Transactional(readOnly = true)
    public List<UsuarioBuscaResponse> buscarUsuariosNaoAssociados(UUID projetoId, String q) {
        permissaoGuard.exigir(projetoId, "papel:administrar");
        if (q == null || q.trim().length() < 3) {
            return List.of();
        }
        return usuarioRepository.buscarNaoAssociados(projetoId, q.trim(), PageRequest.of(0, 20)).stream()
                .map(u -> UsuarioBuscaResponse.builder().id(u.getId()).nome(u.getNome()).email(u.getEmail()).build())
                .toList();
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private void exigirMembro(UUID projetoId) {
        if (!permissaoGuard.membro(projetoId)) {
            throw new AccessDeniedException("Acesso negado");
        }
    }

    private Papel obterPapel(UUID papelId) {
        return papelRepository.findById(papelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "papel não encontrado"));
    }

    private void exigirNaoProtegido(Papel papel) {
        if (papel.isProtegido()) {
            throw new AccessDeniedException("O papel admin é protegido (RN-006)");
        }
    }

    private UUID projetoIdDe(Papel papel) {
        if (papel.getProjeto() == null) {
            // Só ocorre para o papel admin (global) — já bloqueado por exigirNaoProtegido antes.
            throw new AccessDeniedException("O papel admin é protegido (RN-006)");
        }
        return papel.getProjeto().getId();
    }

    private Usuario usuarioAutenticado() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null || !usuario.isAtivo()) {
            throw new AccessDeniedException("Acesso negado");
        }
        return usuario;
    }

    private PapelResponse paraResponse(Papel papel) {
        List<PapelResponse.PermissaoToggle> permissoes = papelPermissaoRepository.findByPapelId(papel.getId()).stream()
                .map(pp -> PapelResponse.PermissaoToggle.builder()
                        .chave(pp.getPermissao().getChave())
                        .habilitada(pp.isHabilitada())
                        .build())
                .toList();
        return PapelResponse.builder()
                .id(papel.getId())
                .chave(papel.getChave())
                .nome(papel.getNome())
                .protegido(papel.isProtegido())
                .permissoes(permissoes)
                .build();
    }
}
