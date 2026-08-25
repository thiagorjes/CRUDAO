package com.crudao.kanban.tarefa;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Estado completo do board para renderização inicial (RF-001). */
@RestController
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/api/projetos/{projetoId}/board")
    public ResponseEntity<BoardResponse> board(@PathVariable UUID projetoId) {
        return ResponseEntity.ok(boardService.board(projetoId));
    }
}
