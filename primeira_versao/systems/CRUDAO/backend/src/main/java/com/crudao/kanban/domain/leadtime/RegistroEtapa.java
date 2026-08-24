package com.crudao.kanban.domain.leadtime;

import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.workflow.Etapa;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Histórico de permanência de uma {@link Tarefa} em uma {@link Etapa} — base do cálculo de
 * lead-time (RF-006, RN-001). {@code saidaEm} nulo indica a permanência atual (etapa em andamento).
 * {@code tempoImpedimentoSegundos} acumula, ao longo desta permanência, a soma dos períodos de
 * {@link Impedimento} já fechados (RN-002).
 */
@Entity
@Table(name = "registro_etapa")
@Getter
@Setter
@NoArgsConstructor
public class RegistroEtapa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "tarefa_id")
  private Tarefa tarefa;

  @ManyToOne(optional = false)
  @JoinColumn(name = "etapa_id")
  private Etapa etapa;

  private Instant entradaEm = Instant.now();

  private Instant saidaEm;

  private long tempoImpedimentoSegundos;
}
