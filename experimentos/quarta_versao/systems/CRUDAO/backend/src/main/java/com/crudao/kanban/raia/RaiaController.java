package com.crudao.kanban.raia;

import com.crudao.kanban.raia.dto.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RaiaController {

    private final RaiaService raiaService;

    @GetMapping("/api/projetos/{projetoId}/raias")
    public ResponseEntity<List<RaiaResponse>> listarRaias(@PathVariable UUID projetoId) {
        return ResponseEntity.ok(raiaService.listarRaias(projetoId));
    }

    @PostMapping("/api/projetos/{projetoId}/raias")
    public ResponseEntity<RaiaResponse> criarRaia(
            @PathVariable UUID projetoId,
            @Valid @RequestBody CriarRaiaRequest request) {
        RaiaResponse resp = raiaService.criarRaia(projetoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PutMapping("/api/raias/{id}")
    public ResponseEntity<RaiaResponse> atualizarRaia(
            @PathVariable UUID id,
            @Valid @RequestBody AtualizarRaiaRequest request) {
        return ResponseEntity.ok(raiaService.atualizarRaia(id, request));
    }

    @DeleteMapping("/api/raias/{id}")
    public ResponseEntity<Void> excluirRaia(@PathVariable UUID id) {
        raiaService.excluirRaia(id);
        return ResponseEntity.noContent().build();
    }
}

