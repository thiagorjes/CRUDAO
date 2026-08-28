package com.crudao.kanban.domain.tarefa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tarefa_impedimento_historico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarefaImpedimentoHistorico {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private Tarefa tarefa;

    @Column(name = "marcado_em", nullable = false)
    private Instant marcadoEm;

    @Column(name = "desmarcado_em")
    private Instant desmarcadoEm;
}

