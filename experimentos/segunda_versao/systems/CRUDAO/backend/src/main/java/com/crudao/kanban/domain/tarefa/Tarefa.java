package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.Workflow;
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

/**
 * Card do board (RF-018). {@code titulo}/{@code descricaoEscopo} ficam congelados após
 * {@code iniciada=true} — bloqueio validado em serviço, não em constraint de banco
 * (`data-model.md`).
 */
@Entity
@Table(name = "tarefa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tarefa {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_atual_id", nullable = false)
    private Etapa etapaAtual;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raia_id", nullable = false)
    private Raia raia;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "descricao_escopo")
    private String descricaoEscopo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @Column(nullable = false)
    private boolean iniciada;

    @Column(nullable = false)
    private boolean impedida;

    @Column(name = "impedida_desde")
    private OffsetDateTime impedidaDesde;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;
}
