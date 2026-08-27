package com.crudao.kanban.domain.leadtime;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposição de tempo por etapa e tempo em impedimento de uma tarefa (RF-006). */
@RestController
@RequestMapping("/api/tarefas/{tarefaId}/registros-etapa")
@RequiredArgsConstructor
public class LeadTimeController {

  private final RegistroEtapaService registroEtapaService;

  @GetMapping
  public List<RegistroEtapaDTO> listar(@PathVariable UUID tarefaId) {
    return registroEtapaService.listarPorTarefa(tarefaId).stream()
        .map(RegistroEtapaDTO::de)
        .toList();
  }
}
