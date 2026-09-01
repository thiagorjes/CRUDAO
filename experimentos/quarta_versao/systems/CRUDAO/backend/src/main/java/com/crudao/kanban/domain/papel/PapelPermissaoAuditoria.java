package com.crudao.kanban.domain.papel;

import com.crudao.kanban.domain.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Registro de auditoria de toggle de {@link PapelPermissao} (RF-016, RN-017). */
@Entity
@Table(name = "papel_permissao_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PapelPermissaoAuditoria {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "papel_id", nullable = false)
    private Papel papel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permissao_id", nullable = false)
    private Permissao permissao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "valor_anterior", nullable = false)
    private boolean valorAnterior;

    @Column(name = "valor_novo", nullable = false)
    private boolean valorNovo;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora = Instant.now();
}
