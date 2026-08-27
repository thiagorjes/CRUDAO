package com.crudao.kanban.domain.workflow;

import jakarta.persistence.Column;
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

/**
 * Coluna do board (RF-010). Nome do campo {@code etapaFinal} — não {@code eFinal} — evita boolean
 * com duas maiúsculas seguidas após o prefixo (`coding-standards.md`).
 */
@Entity
@Table(name = "etapa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Etapa {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int ordem;

    @Column(name = "etapa_final", nullable = false)
    private boolean etapaFinal;
}
