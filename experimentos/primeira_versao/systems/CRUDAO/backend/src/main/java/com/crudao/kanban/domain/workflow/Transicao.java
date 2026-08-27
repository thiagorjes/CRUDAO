package com.crudao.kanban.domain.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Transição permitida entre duas {@link Etapa} de um workflow — RF-002. */
@Entity
@Table(name = "transicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transicao {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "etapa_origem_id")
  private Etapa etapaOrigem;

  @ManyToOne(optional = false)
  @JoinColumn(name = "etapa_destino_id")
  private Etapa etapaDestino;

  @Enumerated(EnumType.STRING)
  private TipoTransicao tipo = TipoTransicao.NORMAL;
}
