package com.crudao.kanban.domain.workflow;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Transição permitida entre duas etapas de um workflow (RF-002, RN-003). */
@Entity
@Table(name = "transicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transicao {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_origem_id", nullable = false)
    private Etapa etapaOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_destino_id", nullable = false)
    private Etapa etapaDestino;
}
