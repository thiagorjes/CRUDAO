package com.crudao.kanban.domain.tarefa;

import com.crudao.kanban.domain.raia.Raia;
import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.Usuario;
import com.crudao.kanban.domain.workflow.Etapa;
import com.crudao.kanban.domain.workflow.Workflow;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tarefa")
@Getter
@Setter
@Builder
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

    @Builder.Default
    @Column(nullable = false)
    private boolean iniciada = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean impedida = false;

    @Column(name = "impedida_desde")
    private Instant impedidaDesde;

    @Builder.Default
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Builder.Default
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();
}
