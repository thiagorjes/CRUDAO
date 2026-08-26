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

/** Base para tempo de impedimento acumulado (RF-006, RN-002). */
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
    private OffsetDateTime marcadoEm;

    @Column(name = "desmarcado_em")
    private OffsetDateTime desmarcadoEm;

    /**
     * Etapa em que o impedimento ocorreu (V11, RF-007) — permite agregar tempo médio de
     * impedimento por etapa no dashboard. Nulo em registros anteriores à migration.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_id")
    private Etapa etapa;
}
