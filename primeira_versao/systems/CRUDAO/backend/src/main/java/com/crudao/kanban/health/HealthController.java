package com.crudao.kanban.health;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de exemplo usado no setup inicial (TASK-00.2) para validar que o frontend consegue
 * chamar o backend. Será substituído por endpoints de domínio nas tasks seguintes.
 */
@RestController
public class HealthController {

  @GetMapping("/api/health")
  public Map<String, Object> health() {
    return Map.of(
        "status", "ok", "service", "kanban-backend", "timestamp", Instant.now().toString());
  }
}
