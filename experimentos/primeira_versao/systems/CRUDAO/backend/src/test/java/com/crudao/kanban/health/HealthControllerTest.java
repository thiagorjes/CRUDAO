package com.crudao.kanban.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class HealthControllerTest {

  @Test
  void deveRetornarStatusOk() {
    Map<String, Object> resultado = new HealthController().health();

    assertThat(resultado).containsEntry("status", "ok").containsEntry("service", "kanban-backend");
  }
}
