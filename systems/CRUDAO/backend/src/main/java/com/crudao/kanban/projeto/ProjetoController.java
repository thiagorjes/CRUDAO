package com.crudao.kanban.projeto;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de projetos incl. finalizar/reabrir (RF-008, RN-015).
 *
 * <p>Autorização delegada ao {@code ProjetoService} — {@code criar} não tem projeto para escopar
 * via {@code @PreAuthorize}; os demais resolvem o projeto do {@code id} do path antes de checar.
 */
@RestController
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping("/api/projetos")
    public List<ProjetoResponse> listar() {
        return projetoService.listarVisiveis();
    }

    @PostMapping("/api/projetos")
    public ResponseEntity<ProjetoResponse> criar(@RequestBody CriarProjetoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.criar(request));
    }

    @PutMapping("/api/projetos/{id}")
    public ProjetoResponse editar(@PathVariable UUID id, @RequestBody EditarProjetoRequest request) {
        return projetoService.editar(id, request);
    }

    @PostMapping("/api/projetos/{id}/finalizar")
    public ProjetoResponse finalizar(@PathVariable UUID id) {
        return projetoService.finalizar(id);
    }

    @PostMapping("/api/projetos/{id}/reabrir")
    public ProjetoResponse reabrir(@PathVariable UUID id) {
        return projetoService.reabrir(id);
    }
}
