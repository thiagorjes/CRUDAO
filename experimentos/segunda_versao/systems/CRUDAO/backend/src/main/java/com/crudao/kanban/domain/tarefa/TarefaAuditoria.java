package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.domain.usuario.Usuario;
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

/** Histórico de auditoria da tarefa (RF-017, RN-016). */
@Entity
@Table(name = "tarefa_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TarefaAuditoria {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private Tarefa tarefa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    /** {@code responsavel}, {@code titulo}, {@code etapa}, {@code impedimento}. */
    @Column(nullable = false, length = 50)
    private String campo;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_novo")
    private String valorNovo;

    @Column(name = "data_hora", nullable = false)
    private OffsetDateTime dataHora;
}
