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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de papéis e toggles de permissão por projeto (RF-013, RF-016, RN-006, RN-017).
 *
 * <p>GETs de listagem por projeto usam {@code @PreAuthorize} com {@code #projetoId} do path;
 * endpoints por {@code papelId} (edição/exclusão/toggle) delegam a autorização ao {@link
 * PapelService}, que resolve o projeto do papel antes de checar a permissão.
 */
@RestController
public class PapelController {

    private final PapelService papelService;

    public PapelController(PapelService papelService) {
        this.papelService = papelService;
    }

    @GetMapping("/api/projetos/{projetoId}/papeis")
    @PreAuthorize("@permissaoGuard.membro(#projetoId)")
    public List<PapelResponse> listar(@PathVariable("projetoId") UUID projetoId) {
        return papelService.listarPorProjeto(projetoId);
    }

    @PostMapping("/api/projetos/{projetoId}/papeis")
    @PreAuthorize("@permissaoGuard.permitido(#projetoId, 'papel:administrar')")
    public ResponseEntity<PapelResponse> criar(
            @PathVariable("projetoId") UUID projetoId, @Valid @RequestBody CriarPapelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(papelService.criar(projetoId, request));
    }

    @PutMapping("/api/papeis/{id}")
    public PapelResponse editar(@PathVariable UUID id, @Valid @RequestBody EditarPapelRequest request) {
        return papelService.editar(id, request);
    }

    @DeleteMapping("/api/papeis/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        papelService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/papeis/{id}/permissoes/{permissaoChave}")
    public PapelResponse togglePermissao(
            @PathVariable UUID id,
            @PathVariable String permissaoChave,
            @RequestBody TogglePermissaoRequest request) {
        return papelService.togglePermissao(id, permissaoChave, request.habilitada());
    }
}
