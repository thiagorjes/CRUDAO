package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.domain.workflow.Etapa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Base para lead-time por etapa (RF-006, RN-001). {@code saidaEm} nulo = etapa em andamento. */
@Entity
@Table(name = "tarefa_etapa_historico")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarefaEtapaHistorico {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private Tarefa tarefa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id", nullable = false)
    private Etapa etapa;

    @Column(name = "entrada_em", nullable = false)
    private OffsetDateTime entradaEm;

    @Column(name = "saida_em")
    private OffsetDateTime saidaEm;
}
