package com.crudao.kanban.tarefa;

import com.crudao.kanban.domain.tarefa.TarefaAuditoria;
import com.crudao.kanban.tarefa.dto.BoardResponse;
import com.crudao.kanban.tarefa.dto.CriarTarefaRequest;
import com.crudao.kanban.tarefa.dto.CriarTarefaResponse;
import com.crudao.kanban.tarefa.dto.EditarTarefaRequest;
import com.crudao.kanban.tarefa.dto.MoverTarefaRequest;
import com.crudao.kanban.tarefa.dto.TarefaDetalheResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TarefaController {

    private final TarefaService tarefaService;
    private final BoardService boardService;

    /**
     * TASK-04.5: Obter board do projeto (etapas, raias, tarefas).
     * GET /api/projetos/{projetoId}/board
     * RF-001: Renderização inicial do board.
     * Sem N+1: queries fixas independente do volume de tarefas.
     */
    @GetMapping("/api/projetos/{projetoId}/board")
    public ResponseEntity<BoardResponse> obterBoard(@PathVariable UUID projetoId) {
        BoardResponse board = boardService.obterBoard(projetoId);
        return ResponseEntity.ok(board);
    }

    /**
     * TASK-04.5: Obter detalhe da tarefa com lead-time por etapa.
     * GET /api/tarefas/{tarefaId}
     * RF-006: Lead-time calculado por etapa, incluindo etapa em andamento.
     */
    @GetMapping("/api/tarefas/{tarefaId}")
    public ResponseEntity<TarefaDetalheResponse> obterDetalhe(@PathVariable UUID tarefaId) {
        TarefaDetalheResponse detalhe = tarefaService.obterComLeadTime(tarefaId);
        return ResponseEntity.ok(detalhe);
    }

    /**
     * TASK-04.2: Mover tarefa entre etapas.
     * POST /api/tarefas/{tarefaId}/mover
     * RF-002: Valida transição configurada.
     * RN-011: Valida `tarefa:finalizar` se etapa final.
     */
    @PostMapping("/api/tarefas/{tarefaId}/mover")
    public ResponseEntity<Void> moverTarefa(
            @PathVariable UUID tarefaId,
            @Valid @RequestBody MoverTarefaRequest request) {
        tarefaService.mover(tarefaId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * TASK-04.2: Editar tarefa com congelamento de campos pós-início.
     * PUT /api/tarefas/{tarefaId}
     * RF-003: Congelamento de `titulo`/`descricaoEscopo` quando `iniciada=true`.
     * RN-012: Validação de autoatribuição de responsável.
     */
    @PutMapping("/api/tarefas/{tarefaId}")
    public ResponseEntity<Void> editarTarefa(
            @PathVariable UUID tarefaId,
            @Valid @RequestBody EditarTarefaRequest request) {
        tarefaService.editar(tarefaId, request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/api/projetos/{projetoId}/tarefas")
    public ResponseEntity<CriarTarefaResponse> criarTarefa(
            @PathVariable UUID projetoId,
            @Valid @RequestBody CriarTarefaRequest request) {
        CriarTarefaResponse resp = tarefaService.criarTarefa(projetoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /**
     * TASK-04.3: Marcar tarefa como impedida.
     * POST /api/tarefas/{tarefaId}/impedimento
     * Requer permissão `tarefa:impedimento`.
     */
    @PostMapping("/api/tarefas/{tarefaId}/impedimento")
    public ResponseEntity<Void> marcarImpedimento(
            @PathVariable UUID tarefaId,
            @RequestParam UUID projetoId) {
        tarefaService.marcarImpedimento(tarefaId, projetoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * TASK-04.3: Desmarcar tarefa como impedida.
     * DELETE /api/tarefas/{tarefaId}/impedimento
     * Requer permissão `tarefa:impedimento`.
     */
    @DeleteMapping("/api/tarefas/{tarefaId}/impedimento")
    public ResponseEntity<Void> desmarcarImpedimento(
            @PathVariable UUID tarefaId,
            @RequestParam UUID projetoId) {
        tarefaService.desmarcarImpedimento(tarefaId, projetoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * TASK-04.4: Excluir tarefa pelo board.
     * DELETE /api/tarefas/{tarefaId}
     * Requer `tarefa:gerenciar` (RN-CB-001).
     * Se dev, requer adicionalmente `tarefa:excluir` habilitada (RN-CB-002).
     * Bloqueado se projeto finalizado (RN-CB-003).
     */
    @DeleteMapping("/api/tarefas/{tarefaId}")
    public ResponseEntity<Void> excluirTarefa(
            @PathVariable UUID tarefaId,
            @RequestParam UUID projetoId) {
        tarefaService.excluirTarefa(tarefaId, projetoId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * TASK-04.4: Obter histórico de auditoria da tarefa.
     * GET /api/tarefas/{tarefaId}/auditoria
     * Retorna todas as alterações relevantes com autor, campo, valores anterior/novo e data/hora.
     * RF-017: rastreabilidade completa de alterações.
     */
    @GetMapping("/api/tarefas/{tarefaId}/auditoria")
    public ResponseEntity<List<TarefaAuditoria>> obterAuditoria(
            @PathVariable UUID tarefaId) {
        List<TarefaAuditoria> auditoria = tarefaService.obterAuditoria(tarefaId);
        return ResponseEntity.ok(auditoria);
    }
}

