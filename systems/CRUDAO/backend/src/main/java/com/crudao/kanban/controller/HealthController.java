package com.crudao.kanban.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint mínimo para validar que o backend está no ar durante o setup (TASK-01.1). */
@RestController
public class HealthController {

    @GetMapping("/api/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
