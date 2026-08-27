package com.crudao.kanban.domain.papel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Toggle de permissão por papel (RF-016) — chave composta (papel, permissão). */
@Entity
@Table(name = "papel_permissao")
@IdClass(PapelPermissaoId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PapelPermissao {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "papel_id")
    private Papel papel;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permissao_id")
    private Permissao permissao;

    @Column(nullable = false)
    private boolean habilitada = false;
}
