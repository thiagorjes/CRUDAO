package com.crudao.kanban.raia;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de {@code Raia} (RF-011).
 *
 * <p>Autorização delegada ao {@code RaiaService} — leitura exige vínculo com o projeto, escrita
 * exige {@code workflow:administrar}/RN-015 ({@code contracts/raias.md}).
 */
@RestController
public class RaiaController {

    private final RaiaService raiaService;

    public RaiaController(RaiaService raiaService) {
        this.raiaService = raiaService;
    }

    @GetMapping("/api/projetos/{projetoId}/raias")
    public List<RaiaResponse> listar(@PathVariable UUID projetoId) {
        return raiaService.listar(projetoId);
    }

    @PostMapping("/api/projetos/{projetoId}/raias")
    public ResponseEntity<RaiaResponse> criar(
            @PathVariable UUID projetoId, @RequestBody CriarRaiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(raiaService.criar(projetoId, request));
    }

    @PutMapping("/api/raias/{id}")
    public RaiaResponse editar(@PathVariable UUID id, @RequestBody EditarRaiaRequest request) {
        return raiaService.editar(id, request);
    }

    @DeleteMapping("/api/raias/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        raiaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
