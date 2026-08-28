package com.crudao.kanban.domain.papel;

import com.crudao.kanban.domain.usuario.Projeto;
import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Associação usuário↔projeto↔papel (RF-015) — chave composta. */
@Entity
@Table(name = "usuario_projeto_papel")
@IdClass(UsuarioProjetoPapelId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioProjetoPapel {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "papel_id")
    private Papel papel;

    @Column(name = "associado_em", nullable = false)
    private OffsetDateTime associadoEm = OffsetDateTime.now();
}
