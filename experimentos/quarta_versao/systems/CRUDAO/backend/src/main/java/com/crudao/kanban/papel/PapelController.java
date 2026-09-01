package com.crudao.kanban.papel;

import com.crudao.kanban.papel.dto.AssociarUsuarioRequest;
import com.crudao.kanban.papel.dto.AtualizarPapelRequest;
import com.crudao.kanban.papel.dto.CriarPapelRequest;
import com.crudao.kanban.papel.dto.PapelResponse;
import com.crudao.kanban.papel.dto.TogglePermissaoRequest;
import com.crudao.kanban.papel.dto.UsuarioBuscaResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * TL-09/TL-10 — Papéis, Permissões e Usuários do Projeto.
 * Contrato: docs/techspec/kanban-tarefas/contracts/papeis-permissoes.md.
 */
@RestController
@RequiredArgsConstructor
public class PapelController {

    private final PapelService papelService;

    @GetMapping("/api/projetos/{projetoId}/papeis")
    public List<PapelResponse> listarPapeis(@PathVariable UUID projetoId) {
        return papelService.listarPapeis(projetoId);
    }

    @PostMapping("/api/projetos/{projetoId}/papeis")
    public ResponseEntity<PapelResponse> criarPapel(
            @PathVariable UUID projetoId, @Valid @RequestBody CriarPapelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(papelService.criarPapel(projetoId, request));
    }

    @PutMapping("/api/papeis/{id}")
    public PapelResponse editarPapel(@PathVariable UUID id, @Valid @RequestBody AtualizarPapelRequest request) {
        return papelService.editarPapel(id, request);
    }

    @DeleteMapping("/api/papeis/{id}")
    public ResponseEntity<Void> excluirPapel(@PathVariable UUID id) {
        papelService.excluirPapel(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/papeis/{id}/permissoes/{permissaoChave}")
    public PapelResponse togglePermissao(
            @PathVariable UUID id,
            @PathVariable String permissaoChave,
            @Valid @RequestBody TogglePermissaoRequest request) {
        return papelService.togglePermissao(id, permissaoChave, request);
    }

    @PostMapping("/api/projetos/{projetoId}/usuarios")
    public ResponseEntity<Void> associarUsuario(
            @PathVariable UUID projetoId, @Valid @RequestBody AssociarUsuarioRequest request) {
        papelService.associarUsuario(projetoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/projetos/{projetoId}/usuarios/{usuarioId}")
    public ResponseEntity<Void> removerUsuario(@PathVariable UUID projetoId, @PathVariable UUID usuarioId) {
        papelService.removerUsuario(projetoId, usuarioId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/projetos/{projetoId}/usuarios/buscar")
    public List<UsuarioBuscaResponse> buscarUsuarios(
            @PathVariable UUID projetoId, @RequestParam(required = false) String q) {
        return papelService.buscarUsuariosNaoAssociados(projetoId, q);
    }
}
