package com.crudao.kanban.papel;

import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.PapelRepository;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.ProjetoRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.usuario.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Associação usuário↔projeto↔papel (RF-015, TL-10). */
@Service
public class UsuarioProjetoPapelService {

    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProjetoRepository projetoRepository;
    private final PapelRepository papelRepository;

    public UsuarioProjetoPapelService(
            UsuarioProjetoPapelRepository usuarioProjetoPapelRepository,
            UsuarioRepository usuarioRepository,
            ProjetoRepository projetoRepository,
            PapelRepository papelRepository) {
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
        this.usuarioRepository = usuarioRepository;
        this.projetoRepository = projetoRepository;
        this.papelRepository = papelRepository;
    }

    public List<UsuarioProjetoResponse> listarPorProjeto(UUID projetoId) {
        Map<UUID, UsuarioProjetoResponseBuilder> porUsuario = new LinkedHashMap<>();
        for (UsuarioProjetoPapel vinculo : usuarioProjetoPapelRepository.findByProjetoId(projetoId)) {
            Usuario usuario = vinculo.getUsuario();
            porUsuario
                    .computeIfAbsent(usuario.getId(), id -> new UsuarioProjetoResponseBuilder(usuario))
                    .papeis
                    .add(vinculo.getPapel().getChave());
        }
        return porUsuario.values().stream()
                .map(b -> new UsuarioProjetoResponse(b.usuario.getId(), b.usuario.getNome(), b.usuario.getEmail(), b.papeis))
                .toList();
    }

    @Transactional
    public void associar(UUID projetoId, AssociarUsuarioRequest request) {
        Projeto projeto =
                projetoRepository
                        .findById(projetoId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "projeto não encontrado"));
        Usuario usuario =
                usuarioRepository
                        .findById(request.usuarioId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "usuário não encontrado"));
        Papel papel =
                papelRepository
                        .findById(request.papelId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "papel não encontrado"));

        // RN-006 — `admin` é global (projeto=null) e protegido: nunca pode ser concedido via este
        // CRUD, sob pena de qualquer `papel:administrar` local escalar para admin do sistema.
        if (papel.isProtegido() || papel.getProjeto() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "papel protegido não pode ser associado");
        }
        if (!papel.getProjeto().getId().equals(projetoId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "papel não pertence a este projeto");
        }

        if (usuarioProjetoPapelRepository
                .findByUsuarioIdAndProjetoIdAndPapelId(usuario.getId(), projetoId, papel.getId())
                .isPresent()) {
            return;
        }

        UsuarioProjetoPapel vinculo = new UsuarioProjetoPapel();
        vinculo.setUsuario(usuario);
        vinculo.setProjeto(projeto);
        vinculo.setPapel(papel);
        vinculo.setAssociadoEm(OffsetDateTime.now());
        usuarioProjetoPapelRepository.save(vinculo);
    }

    @Transactional
    public void remover(UUID projetoId, UUID usuarioId) {
        List<UsuarioProjetoPapel> vinculos =
                usuarioProjetoPapelRepository.findByUsuarioIdAndProjetoId(usuarioId, projetoId);
        usuarioProjetoPapelRepository.deleteAll(vinculos);
    }

    /**
     * Busca usuários ainda não associados ao projeto, para o autocomplete de "associar usuário"
     * (RF-015, TASK-07.5). Escopada por {@code projetoId} (nunca lista a base global de usuários —
     * decisão de arquitetura/segurança desta task) e exige {@code q} com ao menos 3 caracteres para
     * não virar um dump completo da tabela via wildcard vazio.
     */
    public List<UsuarioResumoResponse> buscar(UUID projetoId, String q) {
        String termo = q == null ? "" : q.trim();
        if (termo.length() < 3) {
            return List.of();
        }
        return usuarioRepository.buscarNaoAssociados(projetoId, termo, PageRequest.of(0, 20)).stream()
                .map(u -> new UsuarioResumoResponse(u.getId(), u.getNome(), u.getEmail()))
                .toList();
    }

    private static final class UsuarioProjetoResponseBuilder {
        private final Usuario usuario;
        private final List<String> papeis = new ArrayList<>();

        private UsuarioProjetoResponseBuilder(Usuario usuario) {
            this.usuario = usuario;
        }
    }
}
