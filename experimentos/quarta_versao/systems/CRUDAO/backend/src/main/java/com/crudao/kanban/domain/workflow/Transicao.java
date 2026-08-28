package com.crudao.kanban.domain.workflow;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

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

