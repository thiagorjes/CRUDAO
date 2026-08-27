package com.crudao.kanban.domain.papel;

import com.crudao.kanban.domain.usuario.Projeto;
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
 * Papel de acesso, escopado por {@link Projeto} (RF-013) — exceto {@code admin}, que é global
 * ({@code projeto=null}) e protegido (RN-006).
 */
@Entity
@Table(name = "papel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Papel {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    /** {@code null} = papel global (somente {@code admin}). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @Column(nullable = false)
    private String chave;

    @Column(nullable = false)
    private String nome;

    /** {@code true} somente para {@code admin} — bloqueia edição/exclusão (RN-006). */
    @Column(nullable = false)
    private boolean protegido = false;
}
