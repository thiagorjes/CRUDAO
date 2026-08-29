package com.crudao.kanban.domain.notificacao;

import com.crudao.kanban.domain.tarefa.Tarefa;
import com.crudao.kanban.domain.usuario.Usuario;
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
@Table(name = "notificacao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tarefa_id", nullable = false)
    private Tarefa tarefa;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoNotificacao tipo;

    @Builder.Default
    @Column(nullable = false)
    private boolean lida = false;

    @Builder.Default
    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "lido_em")
    private Instant lidoEm;
}
