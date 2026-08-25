package com.crudao.kanban.papel;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Associação usuário↔projeto↔papel (RF-015, TL-10). */
@RestController
public class UsuarioProjetoController {

    private final UsuarioProjetoPapelService usuarioProjetoPapelService;

    public UsuarioProjetoController(UsuarioProjetoPapelService usuarioProjetoPapelService) {
        this.usuarioProjetoPapelService = usuarioProjetoPapelService;
    }

    @GetMapping("/api/projetos/{projetoId}/usuarios")
    @PreAuthorize("@permissaoGuard.membro(#projetoId)")
    public List<UsuarioProjetoResponse> listar(@PathVariable("projetoId") UUID projetoId) {
        return usuarioProjetoPapelService.listarPorProjeto(projetoId);
    }

    @PostMapping("/api/projetos/{projetoId}/usuarios")
    @PreAuthorize("@permissaoGuard.permitido(#projetoId, 'papel:administrar')")
    public ResponseEntity<Void> associar(
            @PathVariable("projetoId") UUID projetoId, @Valid @RequestBody AssociarUsuarioRequest request) {
        usuarioProjetoPapelService.associar(projetoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/projetos/{projetoId}/usuarios/{usuarioId}")
    @PreAuthorize("@permissaoGuard.permitido(#projetoId, 'papel:administrar')")
    public ResponseEntity<Void> remover(
            @PathVariable("projetoId") UUID projetoId, @PathVariable("usuarioId") UUID usuarioId) {
        usuarioProjetoPapelService.remover(projetoId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
