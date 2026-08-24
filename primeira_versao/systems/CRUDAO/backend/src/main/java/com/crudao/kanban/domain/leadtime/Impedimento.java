package com.crudao.kanban.domain.leadtime;

import com.crudao.kanban.domain.tarefa.Tarefa;
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
 * Período em que uma {@link Tarefa} ficou marcada como impedida, vinculado ao {@link RegistroEtapa}
 * em que ocorreu (RF-004, RN-002). {@code fimEm} nulo indica impedimento ativo.
 */
@Entity
@Table(name = "impedimento")
@Getter
@Setter
@NoArgsConstructor
public class Impedimento {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "tarefa_id")
  private Tarefa tarefa;

  @ManyToOne(optional = false)
  @JoinColumn(name = "registro_etapa_id")
  private RegistroEtapa registroEtapa;

  private Instant inicioEm = Instant.now();

  private Instant fimEm;

  private String motivo;
}
