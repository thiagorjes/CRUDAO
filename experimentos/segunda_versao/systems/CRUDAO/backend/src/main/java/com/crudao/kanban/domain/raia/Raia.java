package com.crudao.kanban.domain.raia;

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
 * Raia (swimlane) de um projeto — agrupamento visual de tarefas no board (RF-011).
 *
 * <p>{@code projeto} {@code null} identifica a raia default global (RN-CB-005), usada quando o
 * card é criado sem raia própria do projeto.
 */
@Entity
@Table(name = "raia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Raia {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private int ordem;
}
