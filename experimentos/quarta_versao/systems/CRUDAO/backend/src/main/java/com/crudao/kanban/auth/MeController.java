package com.crudao.kanban.auth;

import com.crudao.kanban.auth.MeResponse.ProjetoPapeisResponse;
import com.crudao.kanban.domain.papel.Papel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapel;
import com.crudao.kanban.domain.papel.UsuarioProjetoPapelRepository;
import com.crudao.kanban.domain.usuario.Usuario;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code GET /api/me} — usuário autenticado e seus vínculos projeto/papel (RF-014). */
@RestController
public class MeController {

    private final UsuarioProjetoPapelRepository usuarioProjetoPapelRepository;

    public MeController(UsuarioProjetoPapelRepository usuarioProjetoPapelRepository) {
        this.usuarioProjetoPapelRepository = usuarioProjetoPapelRepository;
    }

    @GetMapping("/api/me")
    public ResponseEntity<MeResponse> me() {
        Usuario usuario = UsuarioAutenticadoHolder.get();
        if (usuario == null) {
            // Só ocorre se o AtivoUsuarioFilter não rodou antes deste controller (config errada).
            throw new UsernameNotFoundException("usuario nao resolvido no contexto do request");
        }

        List<UsuarioProjetoPapel> vinculos = usuarioProjetoPapelRepository.findByUsuarioId(usuario.getId());

        Map<UUID, List<String>> papeisPorProjeto = new LinkedHashMap<>();
        for (UsuarioProjetoPapel vinculo : vinculos) {
            UUID projetoId = vinculo.getProjeto().getId();
            Papel papel = vinculo.getPapel();
            papeisPorProjeto.computeIfAbsent(projetoId, id -> new ArrayList<>()).add(papel.getChave());
        }

        List<ProjetoPapeisResponse> projetos =
                papeisPorProjeto.entrySet().stream()
                        .map(entry -> new ProjetoPapeisResponse(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList());

        return ResponseEntity.ok(
                new MeResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.isAdminGlobal(),
                        projetos));
    }
}
